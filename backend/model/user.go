package model

import (
	"backend/utils"
	"context"
	"github.com/pkg/errors"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"golang.org/x/crypto/bcrypt"
	"log"
)

type User struct {
	Id            int         `json:"id" bson:"id"`
	Name          string      `json:"name" bson:"name"`
	Email         string      `json:"email" bson:"email"`
	Password      string      `json:"password" bson:"password"`
	Description   string      `json:"description" bson:"description"`
	Blogs         []int       `json:"blogs" bson:"blogs"`
	Subscriptions []int       `json:"subscriptions" bson:"subscriptions"`
	BlackList     []int       `json:"blacklist" bson:"blacklist"`
	Extension     interface{} `json:"extension" bson:"extension"` //后期前端想展示的其他个人信息字段统一放进来
}

var users *mongo.Collection

func AddUser(user *User) (err error) {
	//完成了邮箱是否重复的验证
	if ExistsUser(bson.D{{"email", user.Email}}) {
		err = errors.New("该邮箱已注册")
		return
	}
	_, err = users.InsertOne(context.Background(), user)
	return
}

func QueryUser(id int) (user *User, err error) {
	user = new(User)
	err = users.FindOne(context.Background(), bson.D{{"id", id}}).Decode(user)
	return user, err
}
func ExistsUser(filter interface{}) bool {
	return users.FindOne(context.Background(), filter).Err() != mongo.ErrNoDocuments
}
func Login(email string, password string) (user *User, err error) {
	user = new(User)
	err = users.FindOne(context.Background(), bson.D{{"email", email}}).Decode(user)
	if err != nil {
		return nil, err
	}
	err = bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password))
	if err != nil {
		return nil, err
	}
	return user, nil
}

// Fetch 将实例信息与数据库中的同步/**
func (user *User) Fetch() (err error) {
	err = users.FindOne(context.Background(), bson.D{{"id", user.Id}}).Decode(user)
	return err
}

// Commit 将实例信息同步到数据库
func (user *User) Commit() (err error) {
	//TODO: 目前的写法性能很差，可以针对不同的操作进行优化
	filter := bson.D{
		{"id", user.Id},
	}
	update := bson.D{
		{"$set", user},
	}
	_, err = users.UpdateOne(context.Background(), filter, update)
	//update, _ := bson.Marshal(user)
	//_, err = users.ReplaceOne(context.Background(), filter, update)

	return err
}

func (user *User) Subscribe(id int) (err error) {
	if user.Subscriptions == nil {
		user.Subscriptions = make([]int, 0)
	}
	if utils.FindInArray(user.Subscriptions, id) == -1 {
		user.Subscriptions = append(user.Subscriptions, id)
		err := user.Commit()
		log.Println(user)
		if err != nil {
			return err
		}
	}
	return nil
}

func (user *User) Unsubscribe(id int) (err error) {
	if user.Subscriptions == nil {
		user.Subscriptions = make([]int, 0)
	}
	if p := utils.FindInArray(user.Subscriptions, id); p != -1 {
		l := len(user.Subscriptions)

		user.Subscriptions[p] = user.Subscriptions[l-1]
		user.Subscriptions = user.Subscriptions[:l-1]
		err := user.Commit()
		log.Println(user)
		if err != nil {
			return err
		}
	}
	return nil
}

func (user *User) Block(id int) (err error) {
	if user.BlackList == nil {
		user.BlackList = make([]int, 0)

	}
	if p := utils.FindInArray(user.BlackList, id); p == -1 {
		user.BlackList = append(user.BlackList, id)
		err := user.Commit()
		if err != nil {
			return err
		}
	}
	return nil
}

func (user *User) Post(blog *Blog) (err error) {
	//单纯存储
	err = AddBlog(blog)
	if err != nil {
		return
	}
	user.Blogs = append(user.Blogs, blog.Id)
	err = user.Commit()
	return
}
