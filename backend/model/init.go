package model

import "backend/store"

func Init() {
	users = store.GetMongo().Database("test").Collection("user")

	initCounter()
}
