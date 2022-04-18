package main

import (
	"backend/model"
	"backend/store"
	"context"
	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/mongo"
	"log"
)

var client *mongo.Client
var err error

func Init() {
	err := store.InitMongo()
	if err != nil {
		log.Fatal(err)
	}
	model.Init()
}
func main() {
	Init()
	engine := gin.Default()
	register(engine)
	client = store.GetMongo() //main函数退出的时候需要关闭数据库
	defer func() {
		if err = client.Disconnect(context.Background()); err != nil {
			panic(err)
		}
	}()

	err = engine.Run()
	if err != nil {
		return
	}

}
