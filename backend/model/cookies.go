package model

import "errors"

var cookies map[string]*User

func init() {
	cookies = make(map[string]*User)
}

func GetUser(cookie string) (*User, error) {
	user, ok := cookies[cookie]
	if !ok {
		return nil, errors.New("Not exists")
	}
	return user, nil
}
func SetCookie(cookie string, user *User) {
	cookies[cookie] = user
}
