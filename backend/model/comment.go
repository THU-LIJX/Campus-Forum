package model

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"time"
)

type Comment struct {
	Id       int       `json:"id" bson:"id"`
	User     int       `json:"user" bson:"user"`
	UserName string    `json:"user_name" bson:"-"`
	Avatar   string    `json:"avatar" bson:"-"`
	Blog     int       `json:"blog" bson:"blog"`
	Time     time.Time `json:"time" bson:"time"`
	Text     string    `json:"text" bson:"text"`
	Liked    int       `json:"liked" bson:"liked"`
	Comment  []int     `json:"comment" bson:"comment"`
}

var comments *mongo.Collection

func AddComment(comment *Comment) (err error) {
	_, err = comments.InsertOne(context.Background(), comment)
	return
}
func GetComment(id int) (comment *Comment, err error) {
	comment = new(Comment)

	err = comments.FindOne(context.Background(), bson.D{{"id", id}}).Decode(comment)
	if err != nil {
		return nil, err
	}
	user, _ := QueryUser(comment.User)
	comment.UserName = user.Name
	comment.Avatar = user.Avatar
	return
}
