package model

import (
	"go.mongodb.org/mongo-driver/mongo"
	"time"
)

type Comment struct {
	Id      int       `json:"id" bson:"id"`
	User    int       `json:"user" bson:"user"`
	Time    time.Time `json:"time" bson:"time"`
	Text    string    `json:"text" bson:"text"`
	Liked   int       `json:"liked" bson:"liked"`
	Comment []int     `json:"comment" bson:"comment"`
}

var comments *mongo.Collection
