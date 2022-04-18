package model

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"golang.org/x/crypto/bcrypt"
)

type User struct {
	Id       int    `json:"id" bson:"id"`
	Name     string `json:"name" bson:"name"`
	Email    string `json:"email" bson:"email"`
	Password string `json:"password" bson:"password"`
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
