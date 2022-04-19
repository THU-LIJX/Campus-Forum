package model

import (
	"context"
	"go.mongodb.org/mongo-driver/mongo"
	"time"
)

//Comment 的实现就是Post。这样可以实现图片评论且评论可以点赞。评论也可以有评论，想要的话
type Blog struct {
	Id       int       `json:"id" bson:"id"`
	User     int       `json:"user" bson:"user"`
	Time     time.Time `json:"time" bson:"time"`
	Text     string    `json:"text" bson:"text"`
	Liked    []int     `json:"liked" bson:"liked"`
	Location string    `json:"location" bson:"location"`
	Comment  []int     `json:"comment" bson:"comment"`
}

var blogs *mongo.Collection

func AddBlog(blog *Blog) (err error) {
	_, err = blogs.InsertOne(context.Background(), blog)
	return
}
