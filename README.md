# easy-subnet

A command line tool to easily split subnets.

## Installation

  * Installation via Homebrew
  ``` bash
  brew tap BrunoBonacci/lazy-tools
  brew install easy-subnet
  ```

  * Install command line tool (Native binary for Mac-OSX and Linux)
  ``` bash
  mkdir -p ~/bin
  wget https://github.com/BrunoBonacci/easy-subnet/releases/download/0.4.1/easy-subnet-$(uname -s)-$(uname -m) -O ~/bin/easy-subnet
  chmod +x ~/bin/easy-subnet
  export PATH=~/bin:$PATH
  ```

  * Install command line tool (for Windows and other platforms, requires Java JRE)
  ``` bash
  mkdir -p ~/bin
  wget https://github.com/BrunoBonacci/easy-subnet/releases/download/0.4.1/easy-subnet -O ~/bin/easy-subnet
  chmod +x ~/bin/easy-subnet
  export PATH=~/bin:$PATH
  ```


## Usage

``` text

     --=  Easy Subnetting Tool =--
  (v0.4.1) - (C) Bruno Bonacci - 2019

 - To subnet a given network:
   easy-subnet -c 10.10.0.0/16 -l '{"dc1" ["net1" "net2"], "dc2" ["net1" "net2" "net3"]}'

 - To list all the IPs of a subnet:
   easy-subnet list -c 10.10.0.0/16
   easy-subnet list --from 192.168.12.1 --to 192.168.15.1

 - Show network details:
   easy-subnet net -c 10.10.0.0/16

Options:
  -c, --cidr CIDR                   CIDR of the subnet to split
  -l, --layout LAYOUT               The layout of how to split the subnets
  -f, --file-layout LAYOUT          The layout of how to split the subnets
  -p, --print SELECTION     :both   Displays a table with the given selection.
                                   Can be one of: `both`, `free`, `nets`, default `both`
  -o, --order ORDER         :net    Diplay ordering: name, net, (default: net)
      --format FORMAT       :table  The format to display the output. One of: `table`, `json`
                                    (default: `table`)
      --from IP                     Starting IP for listing
      --to IP                       Last IP for listing
      --stacktrace                  Display full stacktrace in case of errors
  -h, --help

Please refer to the following page for more information:
https://github.com/BrunoBonacci/easy-subnet

```

The layout is a hierarchical structure representing the desired split.

For example:

``` text
$ easy-subnet  -c 10.12.15.0/24 -l '["net1" "net2"]'

| :name | :network        | :first       | :bcast       | :size |
|-------+-----------------+--------------+--------------+-------|
| net1  | 10.12.15.0/25   | 10.12.15.0   | 10.12.15.127 | 128   |
| net2  | 10.12.15.128/25 | 10.12.15.128 | 10.12.15.255 | 128   |
```

The splits must be power of 2, otherwise it cannot be split equally
and some unused ranges are left.
The layout is expressed in [EDN format](https://github.com/edn-format/edn).


``` text
$ easy-subnet  -c 10.12.15.0/24 -l '["net1" "net2" "net3"]'

| :name  | :network        | :first       | :bcast       | :size |
|--------+-----------------+--------------+--------------+-------|
| net1   | 10.12.15.0/26   | 10.12.15.0   | 10.12.15.63  | 64    |
| net2   | 10.12.15.64/26  | 10.12.15.64  | 10.12.15.127 | 64    |
| net3   | 10.12.15.128/26 | 10.12.15.128 | 10.12.15.191 | 64    |
| free/1 | 10.12.15.192/26 | 10.12.15.192 | 10.12.15.255 | 64    |
```

You can split networks hierarchically as well:

``` text
$ easy-subnet -c 10.10.0.0/16 -l '{"dc1" ["net1" "net2"], "dc2" ["net1" "net2" "net3"]}'

| :name      | :network       | :first      | :bcast        | :size |
|------------+----------------+-------------+---------------+-------|
| dc1.net1   | 10.10.0.0/18   | 10.10.0.0   | 10.10.63.255  | 16384 |
| dc1.net2   | 10.10.64.0/18  | 10.10.64.0  | 10.10.127.255 | 16384 |
| dc2.net1   | 10.10.128.0/19 | 10.10.128.0 | 10.10.159.255 | 8192  |
| dc2.net2   | 10.10.160.0/19 | 10.10.160.0 | 10.10.191.255 | 8192  |
| dc2.net3   | 10.10.192.0/19 | 10.10.192.0 | 10.10.223.255 | 8192  |
| dc2.free/1 | 10.10.224.0/19 | 10.10.224.0 | 10.10.255.255 | 8192  |
```

You can nest many levels:

``` text
$ easy-subnet -c 10.10.0.0/16 -l '
{"mgmt" {"mgmt"   ["az1" "az2" "az3" ]}

 "dev"
 {"public" ["az1" "az2" "az3" ]
  "int-lb" ["az1" "az2" "az3" ]
  "db"     ["az1" "az2" "az3" ]
  "app"    ["az1" "az2" "az3" ]
  "emr"    ["az1" "az2" "az3" ]
  "lambda" ["az1" "az2" "az3" ]}

 "uat"
 {"public" ["az1" "az2" "az3" ]
  "int-lb" ["az1" "az2" "az3" ]
  "db"     ["az1" "az2" "az3" ]
  "app"    ["az1" "az2" "az3" ]
  "emr"    ["az1" "az2" "az3" ]
  "lambda" ["az1" "az2" "az3" ]}

 "prd"
 {"public" ["az1" "az2" "az3" ]
  "int-lb" ["az1" "az2" "az3" ]
  "db"     ["az1" "az2" "az3" ]
  "app"    ["az1" "az2" "az3" ]
  "emr"    ["az1" "az2" "az3" ]
  "lambda" ["az1" "az2" "az3" ]}}'

| :name             | :network       | :first      | :bcast        | :size |
|-------------------+----------------+-------------+---------------+-------|
| mgmt.mgmt.az1     | 10.10.0.0/20   | 10.10.0.0   | 10.10.15.255  | 4096  |
| mgmt.mgmt.az2     | 10.10.16.0/20  | 10.10.16.0  | 10.10.31.255  | 4096  |
| mgmt.mgmt.az3     | 10.10.32.0/20  | 10.10.32.0  | 10.10.47.255  | 4096  |
| mgmt.mgmt.free/1  | 10.10.48.0/20  | 10.10.48.0  | 10.10.63.255  | 4096  |
| dev.public.az1    | 10.10.64.0/23  | 10.10.64.0  | 10.10.65.255  | 512   |
| dev.public.az2    | 10.10.66.0/23  | 10.10.66.0  | 10.10.67.255  | 512   |
| dev.public.az3    | 10.10.68.0/23  | 10.10.68.0  | 10.10.69.255  | 512   |
| dev.public.free/1 | 10.10.70.0/23  | 10.10.70.0  | 10.10.71.255  | 512   |
| dev.int-lb.az1    | 10.10.72.0/23  | 10.10.72.0  | 10.10.73.255  | 512   |
| dev.int-lb.az2    | 10.10.74.0/23  | 10.10.74.0  | 10.10.75.255  | 512   |
| dev.int-lb.az3    | 10.10.76.0/23  | 10.10.76.0  | 10.10.77.255  | 512   |
| dev.int-lb.free/1 | 10.10.78.0/23  | 10.10.78.0  | 10.10.79.255  | 512   |
| dev.db.az1        | 10.10.80.0/23  | 10.10.80.0  | 10.10.81.255  | 512   |
| dev.db.az2        | 10.10.82.0/23  | 10.10.82.0  | 10.10.83.255  | 512   |
| dev.db.az3        | 10.10.84.0/23  | 10.10.84.0  | 10.10.85.255  | 512   |
| dev.db.free/1     | 10.10.86.0/23  | 10.10.86.0  | 10.10.87.255  | 512   |
| dev.app.az1       | 10.10.88.0/23  | 10.10.88.0  | 10.10.89.255  | 512   |
| dev.app.az2       | 10.10.90.0/23  | 10.10.90.0  | 10.10.91.255  | 512   |
| dev.app.az3       | 10.10.92.0/23  | 10.10.92.0  | 10.10.93.255  | 512   |
| dev.app.free/1    | 10.10.94.0/23  | 10.10.94.0  | 10.10.95.255  | 512   |
| dev.emr.az1       | 10.10.96.0/23  | 10.10.96.0  | 10.10.97.255  | 512   |
| dev.emr.az2       | 10.10.98.0/23  | 10.10.98.0  | 10.10.99.255  | 512   |
| dev.emr.az3       | 10.10.100.0/23 | 10.10.100.0 | 10.10.101.255 | 512   |
| dev.emr.free/1    | 10.10.102.0/23 | 10.10.102.0 | 10.10.103.255 | 512   |
| dev.lambda.az1    | 10.10.104.0/23 | 10.10.104.0 | 10.10.105.255 | 512   |
| dev.lambda.az2    | 10.10.106.0/23 | 10.10.106.0 | 10.10.107.255 | 512   |
| dev.lambda.az3    | 10.10.108.0/23 | 10.10.108.0 | 10.10.109.255 | 512   |
| dev.lambda.free/1 | 10.10.110.0/23 | 10.10.110.0 | 10.10.111.255 | 512   |
| dev.free/1.free/1 | 10.10.112.0/21 | 10.10.112.0 | 10.10.119.255 | 2048  |
| dev.free/2.free/2 | 10.10.120.0/21 | 10.10.120.0 | 10.10.127.255 | 2048  |
| uat.public.az1    | 10.10.128.0/23 | 10.10.128.0 | 10.10.129.255 | 512   |
| uat.public.az2    | 10.10.130.0/23 | 10.10.130.0 | 10.10.131.255 | 512   |
| uat.public.az3    | 10.10.132.0/23 | 10.10.132.0 | 10.10.133.255 | 512   |
| uat.public.free/1 | 10.10.134.0/23 | 10.10.134.0 | 10.10.135.255 | 512   |
| uat.int-lb.az1    | 10.10.136.0/23 | 10.10.136.0 | 10.10.137.255 | 512   |
| uat.int-lb.az2    | 10.10.138.0/23 | 10.10.138.0 | 10.10.139.255 | 512   |
| uat.int-lb.az3    | 10.10.140.0/23 | 10.10.140.0 | 10.10.141.255 | 512   |
| uat.int-lb.free/1 | 10.10.142.0/23 | 10.10.142.0 | 10.10.143.255 | 512   |
| uat.db.az1        | 10.10.144.0/23 | 10.10.144.0 | 10.10.145.255 | 512   |
| uat.db.az2        | 10.10.146.0/23 | 10.10.146.0 | 10.10.147.255 | 512   |
| uat.db.az3        | 10.10.148.0/23 | 10.10.148.0 | 10.10.149.255 | 512   |
| uat.db.free/1     | 10.10.150.0/23 | 10.10.150.0 | 10.10.151.255 | 512   |
| uat.app.az1       | 10.10.152.0/23 | 10.10.152.0 | 10.10.153.255 | 512   |
| uat.app.az2       | 10.10.154.0/23 | 10.10.154.0 | 10.10.155.255 | 512   |
| uat.app.az3       | 10.10.156.0/23 | 10.10.156.0 | 10.10.157.255 | 512   |
| uat.app.free/1    | 10.10.158.0/23 | 10.10.158.0 | 10.10.159.255 | 512   |
| uat.emr.az1       | 10.10.160.0/23 | 10.10.160.0 | 10.10.161.255 | 512   |
| uat.emr.az2       | 10.10.162.0/23 | 10.10.162.0 | 10.10.163.255 | 512   |
| uat.emr.az3       | 10.10.164.0/23 | 10.10.164.0 | 10.10.165.255 | 512   |
| uat.emr.free/1    | 10.10.166.0/23 | 10.10.166.0 | 10.10.167.255 | 512   |
| uat.lambda.az1    | 10.10.168.0/23 | 10.10.168.0 | 10.10.169.255 | 512   |
| uat.lambda.az2    | 10.10.170.0/23 | 10.10.170.0 | 10.10.171.255 | 512   |
| uat.lambda.az3    | 10.10.172.0/23 | 10.10.172.0 | 10.10.173.255 | 512   |
| uat.lambda.free/1 | 10.10.174.0/23 | 10.10.174.0 | 10.10.175.255 | 512   |
| uat.free/1.free/1 | 10.10.176.0/21 | 10.10.176.0 | 10.10.183.255 | 2048  |
| uat.free/2.free/2 | 10.10.184.0/21 | 10.10.184.0 | 10.10.191.255 | 2048  |
| prd.public.az1    | 10.10.192.0/23 | 10.10.192.0 | 10.10.193.255 | 512   |
| prd.public.az2    | 10.10.194.0/23 | 10.10.194.0 | 10.10.195.255 | 512   |
| prd.public.az3    | 10.10.196.0/23 | 10.10.196.0 | 10.10.197.255 | 512   |
| prd.public.free/1 | 10.10.198.0/23 | 10.10.198.0 | 10.10.199.255 | 512   |
| prd.int-lb.az1    | 10.10.200.0/23 | 10.10.200.0 | 10.10.201.255 | 512   |
| prd.int-lb.az2    | 10.10.202.0/23 | 10.10.202.0 | 10.10.203.255 | 512   |
| prd.int-lb.az3    | 10.10.204.0/23 | 10.10.204.0 | 10.10.205.255 | 512   |
| prd.int-lb.free/1 | 10.10.206.0/23 | 10.10.206.0 | 10.10.207.255 | 512   |
| prd.db.az1        | 10.10.208.0/23 | 10.10.208.0 | 10.10.209.255 | 512   |
| prd.db.az2        | 10.10.210.0/23 | 10.10.210.0 | 10.10.211.255 | 512   |
| prd.db.az3        | 10.10.212.0/23 | 10.10.212.0 | 10.10.213.255 | 512   |
| prd.db.free/1     | 10.10.214.0/23 | 10.10.214.0 | 10.10.215.255 | 512   |
| prd.app.az1       | 10.10.216.0/23 | 10.10.216.0 | 10.10.217.255 | 512   |
| prd.app.az2       | 10.10.218.0/23 | 10.10.218.0 | 10.10.219.255 | 512   |
| prd.app.az3       | 10.10.220.0/23 | 10.10.220.0 | 10.10.221.255 | 512   |
| prd.app.free/1    | 10.10.222.0/23 | 10.10.222.0 | 10.10.223.255 | 512   |
| prd.emr.az1       | 10.10.224.0/23 | 10.10.224.0 | 10.10.225.255 | 512   |
| prd.emr.az2       | 10.10.226.0/23 | 10.10.226.0 | 10.10.227.255 | 512   |
| prd.emr.az3       | 10.10.228.0/23 | 10.10.228.0 | 10.10.229.255 | 512   |
| prd.emr.free/1    | 10.10.230.0/23 | 10.10.230.0 | 10.10.231.255 | 512   |
| prd.lambda.az1    | 10.10.232.0/23 | 10.10.232.0 | 10.10.233.255 | 512   |
| prd.lambda.az2    | 10.10.234.0/23 | 10.10.234.0 | 10.10.235.255 | 512   |
| prd.lambda.az3    | 10.10.236.0/23 | 10.10.236.0 | 10.10.237.255 | 512   |
| prd.lambda.free/1 | 10.10.238.0/23 | 10.10.238.0 | 10.10.239.255 | 512   |
| prd.free/1.free/1 | 10.10.240.0/21 | 10.10.240.0 | 10.10.247.255 | 2048  |
| prd.free/2.free/2 | 10.10.248.0/21 | 10.10.248.0 | 10.10.255.255 | 2048  |
```

You can separate the free (unused) networks from the ones allocated using
`-p nets` for the allocated ones and `-p free` to display only the unused
ranges.



### List IPs in subnet

If you want to list all the IPs in a given subnet you can run:

``` text
$ easy-subnet list -c 10.12.15.128/28
10.12.15.128
10.12.15.129
10.12.15.130
10.12.15.131
10.12.15.132
10.12.15.133
10.12.15.134
10.12.15.135
10.12.15.136
10.12.15.137
10.12.15.138
10.12.15.139
10.12.15.140
10.12.15.141
10.12.15.142
10.12.15.143
```

Or between two IPs:

``` text
$ easy-subnet list --from 10.12.15.251 --to 10.12.16.6
10.12.15.251
10.12.15.252
10.12.15.253
10.12.15.254
10.12.15.255
10.12.16.0
10.12.16.1
10.12.16.2
10.12.16.3
10.12.16.4
10.12.16.5
10.12.16.6
```

Also in reverse order
``` text
$ easy-subnet list --from 10.12.16.6 --to 10.12.15.251
10.12.16.6
10.12.16.5
10.12.16.4
10.12.16.3
10.12.16.2
10.12.16.1
10.12.16.0
10.12.15.255
10.12.15.254
10.12.15.253
10.12.15.252
10.12.15.251
```

### Show network details

You can display the details of a network via:

``` text
$ easy-subnet net -c 10.12.16.6/20

| :property    | :value                           |
|--------------+----------------------------------|
| network      | 10.12.16.0/20                    |
| type         | :ip4                             |
| first-ip     | 10.12.16.0                       |
| broadcast-ip | 10.12.31.255                     |
| size         | 4096                             |
| network-mask | 255.255.240.0                    |
| bit-mask     | 20                               |
| ip           | 10.12.16.6                       |
| bits:ip      | 00001010000011000001000000000110 |
| bits:bitmask | 11111111111111111111000000000000 |
```


## License

Copyright © 2019 Bruno Bonacci - Distributed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0)
