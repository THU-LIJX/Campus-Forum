package main

import (
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
			c.JSON(403, gin.H{
				"message": "cookie 错误",
			})
		}
		user, err := model.GetUser(cookie)
		if err != nil {
			c.AbortWithStatusJSON(403, gin.H{
				"message": "登陆状态过期",
			})

		}
		c.Set("user", user)
		c.Next()
	}
}
func register(engine *gin.Engine) {
	engine.GET("/ping", pingHandle)
	engine.StaticFile("/favicon.ico", "./asset/images.webp")

	rootGroup := engine.Group("/api")
	userGroup := rootGroup.Group("/user")
	userGroup.Use(Auth())
	userGroup.POST("/change", controller.ChangeUserInfo)
	userGroup.POST("/subscribe", controller.Subscribe)
	userGroup.POST("/unsubscribe", controller.Unsubscribe)
	userGroup.POST("/block", controller.Block)
	userGroup.GET("/info", controller.UserInfo)
	userGroup.POST("/post", controller.Post)

	rootGroup.GET("/info", controller.QueryUser)
	rootGroup.POST("/register", controller.Register)
	rootGroup.POST("/login", controller.Login)
	//Post之类相关的可以设置verify登陆状态的中间件

}
