#include	<stdio.h>
#include	<stdlib.h>
#include	<errno.h>
#include	<netdb.h>
#include	<sys/types.h>
#include	<sys/socket.h>
#include	<netinet/in.h>
#include	<arpa/inet.h>
#include	<string.h>
#include	<time.h>
#include	<signal.h>
#include	<netinet/tcp.h>
#include	<sys/select.h>
#include	<stdbool.h>

#ifdef _MSC_VER
#include <basetsd.h>
#include <Winsock2.h>
#else
#include <pthread.h>
#endif

#define SOCKET_ERROR -1

#define MAXTIME_CONNECT 10

#define	MAX_ANA_NUM		20000
#define	MAX_POL_NUM		50000
#define	MAX_THREAD_NUM		10
#define	NAME_SIZE		60
#define BUF_SIZE 40960
#define BUF_DATANUM 50
#define	CNNAME_SIZE		60

#define TRUE 1
#define FALSE 0

#define	MODE_SEND_ALL	0x01
#define	MODE_SEND_CHANGED	0x02
#define	SEND_POINTINFO	0x11
#define	SEND_RTDATA	0x12
#define	STOP_SEND	0x13

#define	FLAG_FIRST	0x41
#define	FLAG_MIDDLE	0x42
#define	FLAG_LAST	0x43
#define	FLAG_END	0x44

typedef	struct
{	
	char	src_name[NAME_SIZE];
	unsigned char partition;
	char	cnname[CNNAME_SIZE];	
	double	valuesave;
 	time_t  datetime;   
 	unsigned short updateflag;
} ANADEF;

typedef	struct
{	
	char	src_name[NAME_SIZE];
	unsigned char partition;
	char	cnname[CNNAME_SIZE];	
	int	valuesave;
 	time_t  datetime;   
	unsigned short updateflag;
} POLDEF;

typedef	struct
{	
	char	src_name[NAME_SIZE];
	unsigned char partition;	
	char	cnname[CNNAME_SIZE];
	double	valuesave;
 	time_t  datetime;   
 	unsigned short updateflag;
} IMPDEF;

typedef	struct
{
	pthread_t pthreads;	
	void *threadname;
} THREADDEF;

unsigned short	ana_number,pol_number;

ANADEF ana[MAX_ANA_NUM];
POLDEF pol[MAX_POL_NUM];
THREADDEF thread[MAX_THREAD_NUM];


