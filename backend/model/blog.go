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
	MYSELF
)
const (
	TEXT = 1 << iota
	IMAGE
	SOUND
	VIDEO
)

//Comment 的实现就是Post。这样可以实现图片评论且评论可以点赞。评论也可以有评论，想要的话
type Blog struct {
	Id       int       `json:"id" bson:"id"`
	User     int       `json:"user" bson:"user"`
	Time     time.Time `json:"time" bson:"time"`
	Text     string    `json:"text" bson:"text"`
	Type     int       `json:"type" bson:"type"`
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
func GetBlog(id int) (blog *Blog, err error) {
	blog = new(Blog)
	err = blogs.FindOne(context.Background(), bson.D{{"id", id}}).Decode(blog)
	return blog, err
}
func (user *User) ViewBlogs(flags uint16, pagesize, page int64) (res []*Blog, err error) {
	//默认按时间先后
	sortTime, sortLiked := -1, 0
	var filter = bson.M{}
	var sort []bson.E

	if flags&SUBSCIBED != 0 {
		//注意or条件需要使用bson.M的数组而不是bson.D的
		filter["$or"] =

			[]bson.M{
				{"user": user.Id},
				{"user": bson.D{
					{"$in", user.Subscriptions},
					{"$nin", user.BlackList},
				},
				},
			}
	} else if flags&MYSELF != 0 {
		filter["user"] = user.Id
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

func (blog *Blog) Commit() (err error) {
	filter := bson.D{
		{"id", blog.Id},
	}
	update := bson.D{
		{"$set", blog},
	}
	_, err = blogs.UpdateOne(context.Background(), filter, update)
	return err
}

//TODO: 模糊搜索
