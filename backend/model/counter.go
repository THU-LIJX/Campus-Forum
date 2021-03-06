package model

import (
	"backend/config"
	"backend/store"
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
)

//TODO： 全局计数器在内存中存储
type Counter struct {
	Id    string `bson:"id"`
	Value int    `bson:"value"`
}

var counters *mongo.Collection
var usersCounter *Counter
var blogsCounter *Counter
var commentsCounter *Counter
var draftsCounter *Counter
var noticesCounter *Counter

func initCounter() {
	counters = store.GetMongo().Database(config.DB()).Collection("counters")
	usersCounter, _ = getCounter("users")
	blogsCounter, _ = getCounter("blogs")
	commentsCounter, _ = getCounter("comments")
	draftsCounter, _ = getCounter("drafts")
	noticesCounter, _ = getCounter("notices")
}

func GetDraftsCounter() *Counter {
	return draftsCounter
}
func GetUserCounter() *Counter {
	return usersCounter
}
func GetBlogCounter() *Counter {
	return blogsCounter
}

func GetCommentCounter() *Counter {
	return commentsCounter
}

func (c *Counter) Commit() (err error) {
	err = setCounter(c)
	return err
}

func getCounter(id string) (counter *Counter, err error) {
	counter = new(Counter)
	err = counters.FindOne(context.TODO(), bson.D{{"id", id}}).Decode(&counter)
	if err == mongo.ErrNoDocuments {
		counter.Id = id
		counter.Value = 0
		_, err = counters.InsertOne(context.Background(), counter)
		if err != nil {
			return nil, err
		}
	} else if err != nil {
		return nil, err
	}
	return
}

func setCounter(counter *Counter) (err error) {
	update := bson.D{
		{"$set", bson.D{
			{"value", counter.Value},
		}},
	}
	filter := bson.D{
		{"id", counter.Id},
	}
	_, err = counters.UpdateOne(context.Background(), filter, update)
	return
}
