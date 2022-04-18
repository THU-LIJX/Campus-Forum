package store

import (
	"context"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

var client *mongo.Client

func InitMongo() (err error) {
	client, err = mongo.Connect(context.Background(), options.Client().ApplyURI("mongodb://localhost:27017"))
	if err != nil {
		return
	}
	err = client.Ping(context.Background(), nil)
	return
}

func GetMongo() *mongo.Client {
	return client
}
