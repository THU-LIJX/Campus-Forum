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
			c.JSON(403, gin.H{
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

	userGroup := engine.Group("/user")
	userGroup.Use(Auth())
	
	engine.GET("/info", controller.QueryUser)
	engine.POST("/register", controller.Register)
	engine.POST("/login", controller.Login)
	//Post之类相关的可以设置verify登陆状态的中间件

}
