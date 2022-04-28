package model

import (
	"backend/config"
	"backend/store"
)

func Init() {
	users = store.GetMongo().Database(config.DB()).Collection("user")
	blogs = store.GetMongo().Database(config.DB()).Collection("blogs")
	comments = store.GetMongo().Database(config.DB()).Collection("comments")
	drafts = store.GetMongo().Database(config.DB()).Collection("drafts")
	initCounter()
}
