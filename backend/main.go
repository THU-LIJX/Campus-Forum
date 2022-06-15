package main

import (
	"backend/model"
	"backend/store"
	"context"
	"fmt"
	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/mongo"
	"log"
	"net"
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
func getClientIp() string {
	addrs, _ := net.InterfaceAddrs()

	for _, address := range addrs {
		// 检查ip地址判断是否回环地址
		if ipnet, ok := address.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				fmt.Println(ipnet.IP.String())
				return ipnet.IP.String()
			}

		}
	}
	return ""
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

	// 这一句只在本地测试的时候使用，上线的时候注释掉
	// config.SetDomain(getClientIp())

	err = engine.Run("0.0.0.0:8080")
	if err != nil {
		return
	}

}
