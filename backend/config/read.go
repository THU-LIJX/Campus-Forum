package config

import (
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"log"
)

type Conf struct {
	DBConf struct {
		Name string `yaml:"name"`
		Uri  string `yaml:"uri"`
	} `yaml:"dbconf"`
}

var conf Conf

func init() {
	buf, err := ioutil.ReadFile("./config.yaml")

	if err != nil {
		log.Println(err.Error())
		panic("打开配置文件失败")
	}

	err = yaml.Unmarshal(buf, &conf)

	if err != nil {
		log.Println(err.Error())
		panic("解析配置文件失败")
		return
	}
}

func DB() string {
	return conf.DBConf.Name
}
func DBUri() string {
	return conf.DBConf.Uri
}
