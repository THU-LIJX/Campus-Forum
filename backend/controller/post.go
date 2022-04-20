package controller

import (
	"backend/model"
	"github.com/gin-gonic/gin"
	"strconv"
	"time"
)

func GetBlogs(c *gin.Context) {
	//TODO: 暂时先不给别的东西，就测试用
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	var err error
	//筛选要求
	var flags uint16
	sortTime := c.Query("sort_time")
	if sortTime == "ASC" {
		flags |= model.TIMEASC
	} else if sortTime == "DES" {
		flags |= model.TIMEDES
	}

	sortLiked := c.Query("sort_liked")
	if sortLiked == "ASC" {
		flags |= model.LIKEDASC
	} else if sortLiked == "DES" {
		flags |= model.LIKEDDES
	}

	subscribed := c.Query("subscribed")
	if subscribed != "" {
		flags |= model.SUBSCIBED
	}
	myself := c.Query("myself")
	if myself != "" {
		flags |= model.MYSELF
	}
	pagesize, page := 10, 0
	if temp := c.Query("pagesize"); temp != "" {
		pagesize, err = strconv.Atoi(temp)
		if err != nil {
			pagesize = 10
		}
	}
	if temp := c.Query("page"); temp != "" {
		page, err = strconv.Atoi(temp)
		if err != nil {
			page = 0
		}
	}
	res, err := user.ViewBlogs(flags, int64(pagesize), int64(page)) //分页是从0开始的

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
	tp := c.PostForm("type")
	//TODO: 信息格式校验
	var typenum int

	//TODO:这里处理文件上传。文件上传失败就自动停止后续步骤
	switch tp {
	case "text":
		typenum = model.TEXT
	case "image":
		typenum = model.IMAGE
	case "sound":
		typenum = model.SOUND
	case "video":
		typenum = model.VIDEO
	}

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
		LikedBy:  []int{},
		Location: loc,
		Comment:  nil,
		Type:     typenum,
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
