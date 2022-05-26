package model

import (
	"context"
	"fmt"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"strings"
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
	Id          int        `json:"id" bson:"id"`
	User        int        `json:"user" bson:"user"`
	Time        time.Time  `json:"time" bson:"time"`
	Title       string     `json:"title" bson:"title"`
	Text        string     `json:"text" bson:"text"`
	Type        int        `json:"type" bson:"type"`
	Sources     []string   `json:"src" bson:"src"`         //资源列表
	LikedBy     []int      `json:"likedby" bson:"likedby"` //方便排序加的
	Liked       int        `json:"liked" json:"liked"`
	Location    string     `json:"location" bson:"location"`
	Comment     []int      `json:"-" bson:"comment"`
	CommentList []*Comment `json:"comment" bson:"-"`
	UserName    string     `json:"user_name" bson:"-"`
	Avatar      string     `json:"avatar" bson:"-"`
}

type BlogFilter struct {
	Title    string `bson:"title"`
	Content  string `bson:"content"`
	UserName string `bson:"user_name"`
	Type     string `bson:"type"`
}

var blogs *mongo.Collection
var drafts *mongo.Collection

type any interface {
}

func AddBlog(blog *Blog) (err error) {
	_, err = blogs.InsertOne(context.Background(), blog)
	return
}
func GetBlog(id int) (blog *Blog, err error) {
	blog = new(Blog)
	err = blogs.FindOne(context.Background(), bson.D{{"id", id}}).Decode(blog)
	FillBlogs([]*Blog{blog})
	return blog, err
}
func (user *User) ViewBlogs(flags uint16, pagesize, page int64, userId int) (res []*Blog, err error) {
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
	} else {
		filter["$or"] =
			[]bson.M{
				{"user": user.Id},
				{"user": bson.D{
					{"$nin", user.BlackList},
				},
				},
			}
	}
	if userId > 0 {
		filter["user"] = userId
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
	err = FillBlogs(res)
	return
}

func (user *User) SearchBlogs(blogFilter *BlogFilter) (res []*Blog, err error) {
	filter := bson.M{}
	filter["$or"] =
		[]bson.M{
			{"user": user.Id},
			{"user": bson.D{
				{"$nin", user.BlackList},
			},
			},
		}

	makePattern := func(str string) string {
		chars := strings.Split(str, "")
		return ".*" + strings.Join(chars, ".*") + ".*"
	}
	if blogFilter.Content != "" {
		fmt.Println("Filter Content" + blogFilter.Content)
		filter["text"] = bson.M{
			"$regex": primitive.Regex{Pattern: makePattern(blogFilter.Content), Options: "si"}, // i 忽略大小写，s为单行模式，换行也会匹配
		}
	}
	if blogFilter.Title != "" {
		filter["title"] = bson.M{
			"$regex": primitive.Regex{Pattern: makePattern(blogFilter.Title), Options: "si"}, // i 忽略大小写，s为单行模式，换行也会匹配
		}
	}
	if blogFilter.Type != "" {
		switch blogFilter.Type {
		case "text":
			filter["type"] = TEXT
		case "image":
			filter["type"] = IMAGE
		case "sound":
			filter["type"] = SOUND
		case "video":
			filter["type"] = VIDEO
		}
	}

	if blogFilter.UserName != "" {
		userFilter := bson.M{
			"name": bson.M{
				"$regex": primitive.Regex{Pattern: ".*" + blogFilter.UserName + ".*", Options: "i"},
			},
		}
		cursor, err := users.Find(context.Background(), userFilter)
		if err != nil {
			return nil, err
		}
		userIds := make([]int, 0)
		for cursor.Next(context.Background()) {
			var user User
			cursor.Decode(&user)
			userIds = append(userIds, user.Id)
		}
		filter["user"] = bson.M{
			"$in": userIds,
		}
		cursor.Close(context.Background())
	}
	cursor, err := blogs.Find(context.Background(), filter)
	if err != nil {
		return nil, err
	}
	res = make([]*Blog, 0)
	for cursor.Next(context.Background()) {
		var blog Blog
		cursor.Decode(&blog)
		res = append(res, &blog)
	}

	cursor.Close(context.Background())
	err = FillBlogs(res)
	return res, err
}

func FillBlogs(blogs []*Blog) error {
	for _, v := range blogs {
		u, err := QueryUser(v.User)
		if err != nil {

			return err
		}
		v.UserName = u.Name
		v.Avatar = u.Avatar
		v.CommentList = make([]*Comment, 0)
		for _, cid := range v.Comment {
			comment, err := GetComment(cid)
			if err != nil {
				//TODO 目前直接没管
				continue
			}
			v.CommentList = append(v.CommentList, comment)
		}
		if v.Sources == nil {
			v.Sources = make([]string, 0)
		}
	}
	return nil
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
