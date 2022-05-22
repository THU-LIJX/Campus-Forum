package controller

import (
	"backend/config"
	"backend/model"
	"crypto/md5"
	"encoding/hex"
	"github.com/dgrijalva/jwt-go"
	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"golang.org/x/crypto/bcrypt"
	"io"
	"path"
	"strconv"
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
		Verified: false,
	}
	err = model.AddUser(&user)

	if err != nil {
		c.JSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	_ = user.SendValidationEmail() //直接忽略，用户自己可以check

	c.JSON(200, gin.H{
		"message": "ok",
		"id":      counter.Value,
	})
}
func QueryUser(c *gin.Context) {
	var userID int
	if c.Query("myself") != "" {
		cookie, err := c.Cookie("campus_cookie")
		if err != nil {

			c.AbortWithStatusJSON(403, gin.H{
				"message": "cookie 错误",
			})
		}
		userSelf, err := model.GetUser(cookie)

		if err != nil {
			c.AbortWithStatusJSON(403, gin.H{
				"message": "登陆状态过期",
			})
		}
		userID = userSelf.Id
	} else {
		userID, _ = strconv.Atoi(c.Query("id"))
	}

	var user *model.User
	//user.Id = userID
	user, err := model.QueryUser(userID)
	if err != nil {
		c.JSON(400, gin.H{
			"message": err,
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
		return
	}
	//开始生成cookie

	hash := md5.New()
	_, _ = io.WriteString(hash, user.Email+time.Now().String())
	cookie := hex.EncodeToString(hash.Sum(nil))
	//在返回中添加cookie
	c.SetCookie("campus_cookie", cookie, 0, "/", config.Domain(), false, true)
	model.SetCookie(cookie, user)

	c.JSON(200, gin.H{
		"message": "ok",
	})
}

func UserInfo(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	//屏蔽user.Password字段 直接在json的配置里完成

	c.JSON(200, gin.H{
		"message":  "ok",
		"userInfo": user,
	})
}

func ChangeUserInfo(c *gin.Context) {
	//TODO: 还是通过form-data的方式来传
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	name := c.PostForm("name")
	descript := c.PostForm("description")
	user.Name = name
	user.Description = descript
	err := user.Commit()
	if err != nil {
		c.AbortWithStatusJSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
	})
}
func ChangeUserPassword(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	password := c.PostForm("password")
	hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost) //加密
	if err != nil {
		c.JSON(400, gin.H{
			"message": "密码存储错误",
		})
	}
	user.Password = string(hash)
	err = user.Commit()
	if err != nil {
		c.AbortWithStatusJSON(400, gin.H{
			"message": err.Error(),
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "ok",
	})

}
func ChangeAvatar(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	file, err := c.FormFile("img")
	if err != nil {
		c.AbortWithStatusJSON(400, gin.H{
			"message": "图片上传失败",
		})
	}
	suffix := path.Ext(file.Filename)
	filename := "/image/" + strconv.Itoa(user.Id) + suffix
	err = c.SaveUploadedFile(file, config.Static()+filename)
	if err != nil {
		c.AbortWithStatusJSON(400, gin.H{
			"message": "文件上传失败吧",
		})
		return
	}
	user.Avatar = "/static" + filename
	err = user.Commit()
	if err != nil {
		c.AbortWithStatusJSON(400, gin.H{
			"message": "用户信息同步失败",
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
func Logout(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	cookie, _ := c.Cookie("campus_cookie")
	user.Logout(cookie)
	c.JSON(200, gin.H{
		"message": "ok",
	})
}

func SendValidationEmail(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	if user.Verified {
		c.JSON(400, gin.H{
			"message": "已经验证通过了",
		})
		return
	}
	err := user.SendValidationEmail()
	if err != nil {
		c.JSON(400, gin.H{
			"message": "发送失败",
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "发送成功",
	})
}

func VerifyUser(c *gin.Context) {
	token := c.Param("token")
	//id := c.Param("userid")
	stdclaims := new(jwt.StandardClaims)
	tokenClaims, err := jwt.ParseWithClaims(token, stdclaims, func(token *jwt.Token) (interface{}, error) {
		return []byte("campus"), nil
	})
	if err != nil || !tokenClaims.Valid {
		c.JSON(401, gin.H{
			"message": "验证失败",
		})
		return
	}
	userId, err := strconv.Atoi(stdclaims.Id)
	user, err := model.QueryUser(userId)
	err = user.Verify()
	if err != nil {
		c.JSON(401, gin.H{
			"message": "验证失败",
		})
		return
	}
	c.JSON(200, gin.H{
		"message": "验证成功",
	})

}
func GetNotices(c *gin.Context) {
	userI, _ := c.Get("user")
	user := userI.(*model.User)
	statistics, notices, err := user.GetNotices()
	if err != nil {
		c.JSON(500, gin.H{
			"message": err,
		})
		return
	}
	c.JSON(200, gin.H{
		"like":    statistics[model.LIKE],
		"comment": statistics[model.COMMENT],
		"post":    statistics[model.POST],
		"notices": notices,
		"message": "ok",
	})
}
