package controller

import (
	"backend/model"
	"crypto/md5"
	"encoding/hex"
	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"golang.org/x/crypto/bcrypt"
	"io"
	"log"
	"reflect"
	"strconv"
	"strings"
	"time"
)

func Register(c *gin.Context) {
	email := c.PostForm("email")
	name := c.PostForm("name")
	password := c.PostForm("password")
	hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost) //加密
	if err != nil {
		c.JSON(400, gin.H{
			"message": "密码存储错误",
		})
	}
	counter := model.GetUserCounter()
	if err != nil {
		c.JSON(400, gin.H{
			"message": "获取全局用户错误",
		})
	}
	counter.Value++

	err = counter.Commit()
	if err != nil {
		c.JSON(400, gin.H{
			"message": "内部错误",
		})
		return
	}
	user := model.User{
		Id:       counter.Value,
		Name:     name,
		Email:    email,
		Password: string(hash),
	}
	err = model.AddUser(&user)

	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}

	c.JSON(200, gin.H{
		"message": "ok",
		"id":      counter.Value,
	})
}
func QueryUser(c *gin.Context) {

	userID, _ := strconv.Atoi(c.Query("id"))
	var user *model.User
	//user.Id = userID
	user, err := model.QueryUser(userID)
	if err != nil {
		c.JSON(400, gin.H{
			"message": "error",
		})
	}
	c.JSON(200, user)
}

func Login(c *gin.Context) {
	email := c.PostForm("email")
	password := c.PostForm("password")
	user, err := model.Login(email, password)
	if err != nil {
		c.JSON(403, gin.H{
			"message": "用户名或密码错误",
		})
	}
	//开始生成cookie

	hash := md5.New()
	_, _ = io.WriteString(hash, user.Email+time.Now().String())
	cookie := hex.EncodeToString(hash.Sum(nil))
	//在返回中添加cookie
	c.SetCookie("campus_cookie", cookie, 0, "/", "localhost", false, true)
	model.SetCookie(cookie, user)

	c.JSON(200, gin.H{
		"message": "ok",
	})
}

func ChangeUserInfo(c *gin.Context) {
	//TODO: 通过反射直接修改用户的信息
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	m := make(map[string]interface{})
	err := c.BindJSON(&m)

	if err != nil {
		c.JSON(400, gin.H{
			"message": "bind json error",
		})
		return
	}
	log.Println(m)
	//t := reflect.TypeOf(user)

	val := reflect.ValueOf(user).Elem()

	for k, v := range m {
		field := val.FieldByNameFunc(func(s string) bool {
			return strings.ToLower(s) == strings.ToLower(k)
		})
		if field.CanSet() {
			field.Set(reflect.ValueOf(v))
		}
	}
	log.Println(user)
	err = user.Commit()
	if err != nil {
		c.JSON(400, gin.H{
			"message": "user commit error",
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
	})
}
func Unsubscribe(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)

	id, err := strconv.Atoi(c.PostForm("id"))
	if err != nil {
		c.JSON(400, gin.H{
			"message": "参数错误",
		})
		return
	}

	err = user.Unsubscribe(id)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
	})

}
func Subscribe(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)

	id, err := strconv.Atoi(c.PostForm("id"))
	if err != nil {
		c.JSON(400, gin.H{
			"message": "参数错误",
		})
		return
	}
	if !model.ExistsUser(bson.D{{"id", id}}) {
		c.JSON(400, gin.H{
			"message": "该用户不存在",
		})
		return
	}
	err = user.Subscribe(id)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
	})
}

func Block(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)

	id, err := strconv.Atoi(c.PostForm("id"))
	if err != nil {
		c.JSON(400, gin.H{
			"message": "参数错误",
		})
		return
	}
	if !model.ExistsUser(bson.D{{"id", id}}) {
		c.JSON(400, gin.H{
			"message": "该用户不存在",
		})
		return
	}
	err = user.Block(id)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
	})
}
