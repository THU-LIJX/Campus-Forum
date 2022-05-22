package main

import (
	"backend/config"
	"backend/controller"
	"backend/model"
	"github.com/gin-gonic/gin"
)

func pingHandle(c *gin.Context) {
	c.JSON(200, gin.H{
		"message": "pong",
	})
}
func Auth() gin.HandlerFunc {
	return func(c *gin.Context) {
		cookie, err := c.Cookie("campus_cookie")
		if err != nil {

			c.AbortWithStatusJSON(403, gin.H{
				"message": "cookie 错误",
			})
		}
		user, err := model.GetUser(cookie)

		if err != nil {
			c.AbortWithStatusJSON(403, gin.H{
				"message": "登陆状态过期",
			})

		}
		err = user.Fetch()
		if err != nil {
			c.AbortWithStatusJSON(500, gin.H{
				"message": "无法获取用户状态",
			})
		}
		c.Set("user", user)
		c.Next()
	}
}
func Verified() gin.HandlerFunc {
	return func(c *gin.Context) {
		userI, _ := c.Get("user")
		user := userI.(*model.User)
		if !user.Verified {
			c.AbortWithStatusJSON(403, gin.H{
				"message": "请先验证账号再使用此功能",
			})
		}
		c.Next()
	}
}
func register(engine *gin.Engine) {
	engine.GET("/ping", pingHandle)
	engine.StaticFile("/favicon.ico", "./asset/images.webp")
	engine.Static("/static", config.Static())

	rootGroup := engine.Group("/api")
	userGroup := rootGroup.Group("/user")

	userGroup.Use(Auth())
	userGroup.POST("/change/info", controller.ChangeUserInfo)
	userGroup.POST("/change/avatar", controller.ChangeAvatar)
	userGroup.POST("/change/password", controller.ChangeUserPassword)

	userGroup.POST("/subscribe", controller.Subscribe)
	userGroup.POST("/unsubscribe", controller.Unsubscribe)
	userGroup.POST("/block", controller.Block)
	userGroup.GET("/info", controller.UserInfo)
	userGroup.GET("/blogs", controller.GetBlogs)
	userGroup.POST("/logout", controller.Logout)
	userGroup.POST("/validation", controller.SendValidationEmail)
	userGroup.GET("/notices", controller.GetNotices)

	verifiedGroup := userGroup.Group("")
	verifiedGroup.Use(Verified())
	verifiedGroup.POST("/post", controller.Post)
	verifiedGroup.POST("/comment", controller.Comment)
	verifiedGroup.POST("/delcomment", controller.DeleteComment)
	verifiedGroup.POST("/dislike", controller.Dislike)
	verifiedGroup.POST("/like", controller.Like)
	verifiedGroup.GET("/search", controller.Search)

	rootGroup.GET("/info", controller.QueryUser)
	rootGroup.POST("/register", controller.Register)
	rootGroup.POST("/login", controller.Login)
	rootGroup.GET("/comment/:id", controller.GetComment)
	rootGroup.GET("blog/:id", controller.GetSingleBlog)
	//Post之类相关的可以设置verify登陆状态的中间件

	rootGroup.GET("/verify/:token", controller.VerifyUser)
}
