package model

import (
	"backend/utils"
	"context"
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
	Subscriptions []int       `json:"subscriptions" bson:"subscriptions"`
	BlackList     []int       `json:"blackList" bson:"blackList"`
	Extension     interface{} `json:"extension" bson:"extension"` //后期前端想展示的其他个人信息字段统一放进来
}

var users *mongo.Collection

func AddUser(user *User) (err error) {
	_, err = users.InsertOne(context.TODO(), user)
	return
}

func QueryUser(id int) (user *User, err error) {
	user = new(User)
	err = users.FindOne(context.TODO(), bson.D{{"id", id}}).Decode(user)
	return user, err
}
func ExistsUser(id int) bool {
	return users.FindOne(context.Background(), bson.D{{"id", id}}).Err() != mongo.ErrNoDocuments
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

func (user *User) Commit() (err error) {
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
