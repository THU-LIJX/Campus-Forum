package model

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"time"
)

const (
	TIMEASC = 1 << iota //升序
	TIMEDES
	LIKEDASC
	LIKEDDES
	SUBSCIBED
)

//Comment 的实现就是Post。这样可以实现图片评论且评论可以点赞。评论也可以有评论，想要的话
type Blog struct {
	Id       int       `json:"id" bson:"id"`
	User     int       `json:"user" bson:"user"`
	Time     time.Time `json:"time" bson:"time"`
	Text     string    `json:"text" bson:"text"`
	LikedBy  []int     `json:"likedby" bson:"likedby"` //方便排序加的
	Liked    int       `json:"liked" json:"liked"`
	Location string    `json:"location" bson:"location"`
	Comment  []int     `json:"comment" bson:"comment"`
}

var blogs *mongo.Collection

func AddBlog(blog *Blog) (err error) {
	_, err = blogs.InsertOne(context.Background(), blog)
	return
}

func (user *User) ViewBlogs(flags uint16, pagesize, page int64) (res []*Blog, err error) {
	//默认按时间先后
	sortTime, sortLiked := -1, 0
	var filter = bson.M{}
	var sort []bson.E

	if flags&SUBSCIBED != 0 {
		filter = bson.M{
			"age": bson.M{"$in": user.Subscriptions},
		}
	}

	if flags&LIKEDASC != 0 {
		sortLiked = 1
		sort = append(sort, bson.E{"liked", sortLiked})
	}
	if flags&LIKEDDES != 0 {
		sortLiked = -1
		sort = append(sort, bson.E{"liked", sortLiked})
	}
	if flags&TIMEASC != 0 {
		sortTime = 1
	}
	sort = append(sort, bson.E{"time", sortTime})
	//sort := bson.D{
	//	bson.E{"liked", sortLiked},
	//	bson.E{"time", sortTime},
	//}

	skip := page * pagesize
	opt := &options.FindOptions{
		Sort:  sort,
		Limit: &pagesize,
		Skip:  &(skip),
	}
	cursor, err := blogs.Find(context.Background(), filter, opt)

	if err != nil {
		return
	}
	for cursor.Next(context.Background()) {
		var blog Blog
		cursor.Decode(&blog)
		res = append(res, &blog)
	}

	cursor.Close(context.Background())
	return
}
