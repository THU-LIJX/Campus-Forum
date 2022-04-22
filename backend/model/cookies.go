package model

import "errors"

var cookie_user map[string]*User

func init() {
	cookie_user = make(map[string]*User)
}

func GetUser(cookie string) (*User, error) {
	user, ok := cookie_user[cookie]
	if !ok {
		return nil, errors.New("Not exists")
	}
	return user, nil
}
func SetCookie(cookie string, user *User) {
	cookie_user[cookie] = user
}

func DeleteCookie(cookie string) {
	delete(cookie_user, cookie)
}
