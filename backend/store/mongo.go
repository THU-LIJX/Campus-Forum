package store

import (
	"backend/config"
	"context"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"log"
)

var client *mongo.Client

func InitMongo() (err error) {
	log.Println(config.DBUri())
	client, err = mongo.Connect(context.Background(), options.Client().ApplyURI(config.DBUri()))
	if err != nil {
		return
	}
	err = client.Ping(context.Background(), nil)
	return
}

func GetMongo() *mongo.Client {
	return client
}
