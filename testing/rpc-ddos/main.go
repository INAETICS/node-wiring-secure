package main

import (
	"crypto/tls"
	"crypto/x509"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"time"
)

var (
	certFile = flag.String("cert", "ca.pem", "A PEM eoncoded certificate file.")
	keyFile  = flag.String("key", "ca-key.pem", "A PEM encoded private key file.")
	caFile   = flag.String("CA", "ca.pem", "A PEM eoncoded CA's certificate file.")
)

var gcount int
var target = "https://127.0.0.1:6789"

func ddos(client *http.Client, t time.Time) {
	for {
		if gcount%500 == 0 {
			d := time.Since(t).Seconds()
			fmt.Printf("Request: %d, (p.s. %f)\n", gcount, (float64(gcount) / d))
		}
		gcount++
		// Do GET something
		resp, err := client.Get(target)
		if err != nil {
			fmt.Println(err)
			// log.Fatal(err)
		}
		// defer resp.Body.Close()
		resp.Body.Close()
	}
}

func waitForInput() {
	for {
		time.Sleep(2 * time.Millisecond)
	}
}

func main() {
	ddosThreads := 10

	flag.Parse()

	// Load client cert
	cert, err := tls.LoadX509KeyPair(*certFile, *keyFile)
	if err != nil {
		log.Fatal(err)
	}

	// Load CA cert
	caCert, err := ioutil.ReadFile(*caFile)
	if err != nil {
		log.Fatal(err)
	}
	caCertPool := x509.NewCertPool()
	caCertPool.AppendCertsFromPEM(caCert)

	// Setup HTTPS client
	tlsConfig := &tls.Config{
		Certificates:       []tls.Certificate{cert},
		RootCAs:            caCertPool,
		InsecureSkipVerify: true,
	}
	tlsConfig.BuildNameToCertificate()
	transport := &http.Transport{TLSClientConfig: tlsConfig}

	gcount = 0
	nowtime := time.Now()

	for i := 0; i < ddosThreads; i++ {
		client := &http.Client{Transport: transport}
		go ddos(client, nowtime)
	}

	waitForInput()

	// // Dump response
	// data, err := ioutil.ReadAll(resp.Body)
	// if err != nil {
	// 	log.Fatal(err)
	// }
	// log.Println(string(data))
}
