package model

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"log"
	"time"
)

const (
	LIKE    = "like"
	COMMENT = "comment"
	POST    = "post"
)

type Notice struct {
	Id       int       `json:"id" bson:"id"`
	User     int       `bson:"user" json:"user"` //用户id,发出通知的用户
	UserName string    `bson:"user_name" json:"user_name"`
	Avatar   string    `json:"avatar" bson:"avatar"`
	Blog     int       `json:"blog" bson:"blog"`
	Time     time.Time `json:"time" bson:"time"`
	Action   string    `json:"action" bson:"action"` // like 点赞 comment 评论 post 发布
}

var notices *mongo.Collection

func AddNotice(notice *Notice) (id int, err error) {
	noticesCounter.Value++
	notice.Id = noticesCounter.Value
	noticesCounter.Commit()
	_, err = notices.InsertOne(context.Background(), notice)
	return noticesCounter.Value, err
}

func SendNotice(receiver []int, userId int, blogId int, action string) error {
	if receiver == nil || len(receiver) == 0 {
		return nil
	}
	user, err := QueryUser(userId)
	if err != nil {
		return err
	}
	if user.Notices == nil {
		user.Notices = make([]int, 0)
		err = user.Commit()
		if err != nil {
			return err
		}
	}
	notice := &Notice{
		User:     userId,
		UserName: user.Name,
		Avatar:   user.Avatar,
		Blog:     blogId,
		Time:     time.Now(),
		Action:   action,
	}
	noticeId, err := AddNotice(notice)
	if err != nil {
		return err
	}

	for _, uid := range receiver {
		err = AddNoticeToUser(noticeId, uid)
		if err != nil {
			return err
		}
	}
	return nil
}

func (user *User) GetNotices() (map[string]int, []*Notice, error) {
	//返回的包括这个用户所有的通知以及对新通知的统计
	err := user.Fetch()
	if err != nil {
		return nil, nil, err
	}
	statistic := make(map[string]int)
	log.Println(user.Notices)
	filter := bson.M{
		"id": bson.M{"$in": user.Notices},
	}
	opt := &options.FindOptions{
		Sort: []bson.E{{"time", -1}},
	}
	res := make([]*Notice, 0)
	cursor, err := notices.Find(context.Background(), filter, opt)
	if err != nil {
		return nil, nil, err
	}
	for cursor.Next(context.Background()) {
		var notice Notice
		cursor.Decode(&notice)
		log.Printf("notice: %+v", notice)
		res = append(res, &notice)
		if notice.Time.After(user.LastNoticed) {
			statistic[notice.Action]++
		}
	}
	cursor.Close(context.Background())
	user.LastNoticed = time.Now()
	user.Commit()
	return statistic, res, nil
}
