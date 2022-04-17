package main

import (
	"context"
	"fmt"
	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"time"
)

type User struct {
	Id    string `json:"id"`
	Name  string `json:"name"`
	Email string `json:"email"`
}

var client *mongo.Client

func pingHandle(c *gin.Context) {
	c.JSON(200, gin.H{
		"message": "pong",
	})
}

func queryUser(c *gin.Context) {

	userID := c.Query("id")
	var user User
	//user.Id = userID
	collection := client.Database("test").Collection("user")
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	err := collection.FindOne(ctx, bson.M{"id": userID}).Decode(&user)
	if err != nil {
		c.JSON(400, gin.H{
			"message": "error",
		})
	}
	c.JSON(200, user)
}
func main() {
	engine := gin.Default()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	client, err := mongo.Connect(ctx, options.Client().ApplyURI("mongodb://localhost:27017"))
	if err != nil {
		fmt.Println(err.Error())
	}
	defer func() {
		if err = client.Disconnect(ctx); err != nil {
			panic(err)
		}
	}()

	engine.GET("/ping", pingHandle)
	engine.GET("/user", queryUser)
	err = engine.Run()
	if err != nil {
		return
	}

}
