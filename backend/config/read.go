package config

import (
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"log"
	"os"
)

type Conf struct {
	DBConf struct {
		Name string `yaml:"name"`
		Uri  string `yaml:"uri"`
	} `yaml:"dbconf"`
	HostConf struct {
		Domain string `yaml:"domain"`
		Static string `yaml:"static"`
	} `yaml:"hostconf"`
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
	want2make := []string{"/image", "/sound", "/video"}
	for _, s := range want2make {
		_, err := os.Stat(Static() + s)
		if os.IsNotExist(err) {
			err := os.MkdirAll(Static()+s, 0775)
			if err != nil {
				log.Println(err.Error())
				panic("创建静态文件夹失败")
				return
			}
		}
	}
}

func DB() string {
	return conf.DBConf.Name
}
func DBUri() string {
	return conf.DBConf.Uri
}
func Domain() string {
	return conf.HostConf.Domain
}
func Static() string {
	return conf.HostConf.Static
}
