package utils

func FindInArray(arr []int, target int) int {
	for i, v := range arr {
		if target == v {
			return i
		}
	}

	return -1
}
