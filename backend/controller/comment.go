package controller

import (
	"backend/model"
	"github.com/gin-gonic/gin"
	"strconv"
	"time"
)

func GetComment(c *gin.Context) {
	idstr := c.Param("id")
	id, _ := strconv.Atoi(idstr)
	comment, err := model.GetComment(id)
	if err != nil {
		c.JSON(404, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
		"comment": comment,
	})

}
func Comment(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	now := time.Now()
	text := c.PostForm("text")
	blogIds := c.PostForm("blog")
	blogID, err := strconv.Atoi(blogIds)
	//TODO:添加参数验证

	blog, err := model.GetBlog(blogID)
	if err != nil {
		c.JSON(500, gin.H{
			"message": "找不到此动态",
		})
		return
	}
	counter := model.GetCommentCounter()
	counter.Value++
	err = counter.Commit()
	if err != nil {
		c.JSON(500, gin.H{
			"message": "评论失败，系统内部错误",
		})
		return
	}
	comment := model.Comment{
		Id:      counter.Value,
		User:    user.Id,
		Time:    now,
		Text:    text,
		Liked:   0,
		Comment: nil,
		Blog:    blogID,
	}
	err = user.Comment(blog, &comment)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	blog, err = model.GetBlog(blogID)
	blog.CommentList = make([]*model.Comment, 0)
	for _, cid := range blog.Comment {
		comment, err := model.GetComment(cid)
		if err != nil {
			//TODO 目前直接没管
			continue
		}
		blog.CommentList = append(blog.CommentList, comment)
	}
	c.JSON(200, gin.H{
		"message":  "ok",
		"time":     now,
		"comments": blog.CommentList,
	})
}

func DeleteComment(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	idstr := c.PostForm("id")
	id, _ := strconv.Atoi(idstr)
	comment, err := model.GetComment(id)
	blogID := comment.Blog
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	err = user.DeleteComment(comment)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	blog, err := model.GetBlog(blogID)
	blog.CommentList = make([]*model.Comment, 0)
	for _, cid := range blog.Comment {
		comment, err := model.GetComment(cid)
		if err != nil {
			//TODO 目前直接没管
			continue
		}
		blog.CommentList = append(blog.CommentList, comment)
	}
	c.JSON(200, gin.H{
		"message":  "ok",
		"comments": blog.CommentList,
	})

}

func Like(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	blogstr := c.PostForm("blog")
	blogid, _ := strconv.Atoi(blogstr)
	blog, err := model.GetBlog(blogid)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	err, likedby := user.Like(blog)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
		"likedby": likedby,
	})
}

func Dislike(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	blogstr := c.PostForm("blog")
	blogid, _ := strconv.Atoi(blogstr)
	blog, err := model.GetBlog(blogid)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	err, likedby := user.Dislike(blog)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
		"likedby": likedby,
	})
}
