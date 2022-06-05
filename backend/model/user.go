package model

import (
	"backend/config"
	"backend/utils"
	"context"
	"fmt"
	"github.com/dgrijalva/jwt-go"
	"github.com/jordan-wright/email"
	"github.com/pkg/errors"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"golang.org/x/crypto/bcrypt"
	"log"
	"net/smtp"
	"strconv"
	"time"
)

type User struct {
	Id            int         `json:"id" bson:"id"`
	Name          string      `json:"name" bson:"name"`
	Email         string      `json:"email" bson:"email"`
	Password      string      `bson:"password" json:"-"`
	Description   string      `json:"description" bson:"description"`
	Avatar        string      `json:"avatar" bson:"avatar"`
	Verified      bool        `json:"verified" bson:"verified"`
	Blogs         []int       `json:"blogs" bson:"blogs"`
	Drafts        []int       `json:"drafts" json:"drafts"`
	Subscriptions []int       `json:"subscriptions" bson:"subscriptions"`
	BlackList     []int       `json:"blacklist" bson:"blacklist"`
	Notices       []int       `json:"notices" bson:"notices"`
	Subscribed    []int       `json:"subscribed" bson:"subscribed"`
	Extension     interface{} `json:"extension" bson:"extension"` //后期前端想展示的其他个人信息字段统一放进来
	LastNoticed   time.Time   `bson:"last_noticed" json:"last_noticed"`
}

var users *mongo.Collection

func AddUser(user *User) (err error) {
	//完成了邮箱是否重复的验证
	if ExistsUser(bson.D{{"email", user.Email}}) {
		err = errors.New("该邮箱已注册")
		return
	}
	_, err = users.InsertOne(context.Background(), user)
	return
}

func QueryUser(id int) (user *User, err error) {
	user = new(User)
	err = users.FindOne(context.Background(), bson.D{{"id", id}}).Decode(user)
	if user.Blogs == nil {
		user.Blogs = make([]int, 0)
	}
	if user.Subscriptions == nil {
		user.Subscriptions = make([]int, 0)
	}
	if user.Drafts == nil {
		user.Drafts = make([]int, 0)
	}
	if user.BlackList == nil {
		user.BlackList = make([]int, 0)
	}
	if user.Subscribed == nil {
		user.Subscribed = make([]int, 0)
	}
	if user.Notices == nil {
		user.Notices = make([]int, 0)
	}
	err = user.Commit()
	return user, err
}
func ExistsUser(filter interface{}) bool {
	return users.FindOne(context.Background(), filter).Err() != mongo.ErrNoDocuments
}
func Login(email string, password string) (user *User, err error) {
	user = new(User)
	err = users.FindOne(context.Background(), bson.D{{"email", email}}).Decode(user)
	if err != nil {
		return nil, err
	}
	err = bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password))
	if err != nil {
		return nil, err
	}

	//确保用户的数据是非nil的
	if user.Subscriptions == nil {
		user.Subscriptions = make([]int, 0)
	}
	if user.BlackList == nil {
		user.BlackList = make([]int, 0)
	}
	if user.Drafts == nil {
		user.Drafts = make([]int, 0)
	}
	if user.BlackList == nil {
		user.BlackList = make([]int, 0)
	}
	if user.Subscribed == nil {
		user.Subscribed = make([]int, 0)
	}
	if user.Notices == nil {
		user.Notices = make([]int, 0)
	}
	user.Commit()
	return user, nil
}

// Fetch 将实例信息与数据库中的同步/**
func (user *User) Fetch() (err error) {
	err = users.FindOne(context.Background(), bson.D{{"id", user.Id}}).Decode(user)
	return err
}

// Commit 将实例信息同步到数据库
func (user *User) Commit() (err error) {
	//TODO: 目前的写法性能很差，可以针对不同的操作进行优化
	filter := bson.D{
		{"id", user.Id},
	}
	update := bson.D{
		{"$set", user},
	}
	_, err = users.UpdateOne(context.Background(), filter, update)
	//update, _ := bson.Marshal(user)
	//_, err = users.ReplaceOne(context.Background(), filter, update)

	return err
}

func (user *User) Subscribe(id int) (err error) {
	fmt.Println("start subscribe")
	if user.Subscriptions == nil {
		user.Subscriptions = make([]int, 0)
		err = user.Commit()
		if err != nil {
			return err
		}
	}
	_, err = users.UpdateOne(context.Background(), bson.D{{"id", user.Id}}, bson.D{
		{"$addToSet", bson.M{"subscriptions": id}},
	})
	if err != nil {
		log.Printf("[Subscribe]Fail to update subscriptions,error=%+v", err)
		return err
	}
	user2, err := QueryUser(id)
	if err != nil {
		return err
	}
	_, err = users.UpdateOne(context.Background(), bson.D{{"id", user2.Id}}, bson.D{
		{"$addToSet", bson.M{"subscribed": user.Id}},
	})
	if err != nil {
		return err
	}
	user.Fetch()
	//if user.Subscriptions == nil {
	//	user.Subscriptions = make([]int, 0)
	//}
	//if utils.FindInArray(user.Subscriptions, id) == -1 {
	//	user.Subscriptions = append(user.Subscriptions, id)
	//	err := user.Commit()
	//	log.Println(user)
	//	if err != nil {
	//		return err
	//	}
	//}
	return nil
}

func (user *User) Unsubscribe(id int) (err error) {
	_, err = users.UpdateOne(context.Background(), bson.D{{"id", user.Id}}, bson.D{
		{"$pull", bson.M{"subscriptions": id}},
	})
	if err != nil {
		return err
	}
	user2, err := QueryUser(id)
	if err != nil {
		return err
	}
	_, err = users.UpdateOne(context.Background(), bson.D{{"id", user2.Id}}, bson.D{
		{"$pull", bson.M{"subscribed": user.Id}},
	})
	if err != nil {
		return err
	}
	user.Fetch()
	//if user.Subscriptions == nil {
	//	user.Subscriptions = make([]int, 0)
	//}
	//if p := utils.FindInArray(user.Subscriptions, id); p != -1 {
	//	l := len(user.Subscriptions)
	//
	//	user.Subscriptions[p] = user.Subscriptions[l-1]
	//	user.Subscriptions = user.Subscriptions[:l-1]
	//	err := user.Commit()
	//	log.Println(user)
	//	if err != nil {
	//		return err
	//	}
	//}
	return nil
}

func (user *User) Block(id int) (err error) {
	if user.BlackList == nil {
		user.BlackList = make([]int, 0)

	}
	if p := utils.FindInArray(user.BlackList, id); p == -1 {
		user.BlackList = append(user.BlackList, id)
		err := user.Commit()
		if err != nil {
			return err
		}
	}
	return nil
}

func (user *User) Post(blog *Blog) (err error) {
	//单纯存储
	err = user.Fetch()
	if err != nil {
		return err
	}
	err = AddBlog(blog)
	if err != nil {
		return
	}
	user.Blogs = append(user.Blogs, blog.Id)
	err = user.Commit()
	if err != nil {
		return err
	}

	return SendNotice(user.Subscribed, user.Id, blog.Id, POST)
}

//TODO: 点赞，评论
func (user *User) Comment(blog *Blog, comment *Comment) (err error) {
	//TODO: 被屏蔽的用户不能评论
	blog.Comment = append(blog.Comment, comment.Id)
	err = blog.Commit()
	if err != nil {
		return err
	}
	//对Blog操作错误会导致评论也不插入到评论里

	err = AddComment(comment)
	if err != nil {
		return err
	}
	// Notice

	return SendNotice([]int{blog.User}, user.Id, blog.Id, COMMENT)
}
func (user *User) DeleteComment(comment *Comment) (err error) {

	//有两种情况下有权限删除：自己给别人留下的评论；自己blog下的任何评论
	blog, err := GetBlog(comment.Blog)
	if err != nil {
		return
	}
	if comment.User != user.Id && blog.User != user.Id {
		return errors.New("无权删除评论")
	}
	//开始删除的逻辑
	_, err = comments.DeleteOne(context.Background(), bson.D{{"id", comment.Id}})
	if err != nil {
		return err
	}
	_, err = blogs.UpdateOne(context.Background(), bson.D{{"id", blog.Id}}, bson.D{{"$pull", bson.M{"comment": comment.Id}}})
	return
}

//TODO: 优化liked
func (user *User) Like(blog *Blog) (err error, likedby []int) {
	_, err = blogs.UpdateOne(context.Background(), bson.D{{"id", blog.Id}}, bson.D{{"$addToSet", bson.M{"likedby": user.Id}}})
	if err != nil {
		log.Println("添加id失败")
	}
	blog, _ = GetBlog(blog.Id)
	likedby = blog.LikedBy
	blog.Liked = len(blog.LikedBy)
	_, err = blogs.UpdateOne(context.Background(), bson.D{{"id", blog.Id}}, bson.D{{"$set", bson.M{"liked": blog.Liked}}})
	if err != nil {
		return err, nil
	}
	// Notice
	err = SendNotice([]int{blog.User}, user.Id, blog.Id, LIKE)
	return err, likedby
}
func (user *User) Dislike(blog *Blog) (err error, likedby []int) {
	_, err = blogs.UpdateOne(context.Background(), bson.D{{"id", blog.Id}}, bson.D{{"$pull", bson.M{"likedby": user.Id}}})
	blog, _ = GetBlog(blog.Id)
	likedby = blog.LikedBy
	blog.Liked = len(blog.LikedBy)
	_, err = blogs.UpdateOne(context.Background(), bson.D{{"id", blog.Id}}, bson.D{{"$set", bson.M{"liked": blog.Liked}}})
	return
}

//TODO: Logout 把不要的cookie删除掉
func (user *User) Logout(cookie string) {
	DeleteCookie(cookie)

}

func (user *User) Verify() error {
	_, err := users.UpdateOne(context.Background(), bson.D{{"id", user.Id}}, bson.D{{"$set", bson.M{"verified": true}}})
	return err
}

func (user *User) SendValidationEmail() (err error) {
	now := time.Now()
	expiry := now.Add(time.Minute * 60) //一个小时内有效
	claims := &jwt.StandardClaims{
		ExpiresAt: expiry.Unix(),
		Id:        strconv.Itoa(user.Id),
	}
	token, err := jwt.NewWithClaims(jwt.SigningMethodHS256, claims).SignedString([]byte("campus"))
	//log.Println(err.Error())
	log.Println(token)
	//发送邮件
	e := email.NewEmail()
	e.From = "Campusbbs <1403292286@qq.com>"
	e.To = []string{user.Email}
	e.Subject = "验证用户身份"
	e.Text = []byte("http://" + config.Domain() + ":8080/api/verify/" + token) //TODO 这里硬编码了端口
	err = e.Send("smtp.qq.com:25", smtp.PlainAuth("", "1403292286@qq.com", "kpcimoxvgthdgifc", "smtp.qq.com"))
	return err
}

func AddNoticeToUser(noticeId, userId int) error {
	user, err := QueryUser(userId)

	_, err = users.UpdateOne(context.Background(), bson.D{{"id", user.Id}}, bson.D{
		{"$push", bson.M{"notices": noticeId}},
	})
	return err
}
