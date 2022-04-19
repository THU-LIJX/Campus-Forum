package controller

import (
	"backend/model"
	"github.com/gin-gonic/gin"
	"time"
)

func GetBlogs(c *gin.Context) {
	//TODO: 暂时先不给别的东西，就测试用
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	res, err := user.ViewBlogs(0, 2, 2)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
		"blogs":   res,
	})
}

//需要验证用户状态
func Post(c *gin.Context) {
	//需要读取post上来的内容，包含文字图片等等信息。打上时间标签
	//依旧采用form-data格式。文件直接可以读出来
	//TODO:信息校验
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	now := time.Now()
	text := c.PostForm("text")
	loc := c.PostForm("location")
	//TODO:这里处理文件上传。文件上传失败就自动停止后续步骤

	//动态核心部分上传
	counter := model.GetBlogCounter()
	counter.Value++
	err := counter.Commit()
	if err != nil {
		c.JSON(500, gin.H{
			"message": "发布失败，系统内部错误",
		})
		return
	}
	blog := model.Blog{
		Id:       counter.Value,
		User:     user.Id,
		Time:     now,
		Text:     text,
		Liked:    0,
		LikedBy:  nil,
		Location: loc,
		Comment:  nil,
	}
	err = user.Post(&blog)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
		"time":    now,
	})
}
