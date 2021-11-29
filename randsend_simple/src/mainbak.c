#include	"../inc/randSend.h"
#include	"../inc/cJSON.h"

#define NUM_INFO 0
#define NUM_DATA 1
#define STA_INFO 2
#define STA_DATA 3


/*ANAINFO_STR、ANADATA_STR 结构类型为ANA、PAM、IMP共用*/
typedef struct
{
	char name[32];	
	float  val;
//	char	qual;
	char describe[60];
//	time_t time;
}ANAINFO_STR;

typedef struct
{
	char name[32];
	float  val;
//	char	qual;
}ANADATA_STR;

/*STATINFO_STR、STATDATA_STR 结构类型为POL、INTE、SYS共用*/
typedef struct
{
	char name[32];
	ushort  val;
//	char	qual;
	char describe[60];
//	int time;
}STATINFO_STR;

typedef struct
{
	char name[32];
	ushort val;
	//	char	qual;
	int	time;
}STATDATA_STR;


ANADEF ana[MAX_ANA_NUM], ana_chg[MAX_ANA_NUM];
POLDEF pol[MAX_POL_NUM], pol_chg[MAX_POL_NUM];
static pthread_mutex_t mutex;

static pthread_mutex_t mutex;
static unsigned in_shutdown = 0;
static int mode = MODE_SEND_CHANGED;

int serv_sock;

void createSendObjects(int type, int count, void *p)
{
	cJSON *root, *list, *data, *thm, *fld;
	char *out;
	int i, j;
	time_t timestamp;
	void *pbuf;
	ANADEF *ana;
	POLDEF *stat;

	if ((type == NUM_INFO)||(type == NUM_DATA))
	{		
		ana = (ANADEF *)p;
	}	
	else if((type == STA_INFO)||(type == STA_DATA))
	{		
		stat = (POLDEF *)p;
	}	

	j = 0;
	time(&timestamp);
	
	for (i = 0; i < count; i++)
	{
		if (j == 0)
		{
			root = cJSON_CreateObject();
			cJSON_AddNumberToObject(root, "type", type);
			if ((type == NUM_INFO) || (type == NUM_DATA))
				cJSON_AddNumberToObject(root, "time", timestamp);    //模拟量、参数、电度量统一打系统时标
			cJSON_AddItemToObject(root, "list", list = cJSON_CreateArray());
		}
		cJSON_AddItemToArray(list, fld = cJSON_CreateObject());
		if (type == NUM_INFO)
		{
			cJSON_AddStringToObject(fld, "id", ana[i].src_name);
			cJSON_AddStringToObject(fld, "cn", ana[i].cnname);
			cJSON_AddNumberToObject(fld, "v", ana[i].valuesave);
			cJSON_AddNumberToObject(fld, "q", ana[i].updateflag);
		}
		else if (type == NUM_DATA)
		{
			cJSON_AddStringToObject(fld, "id", ana[i].src_name);
			cJSON_AddNumberToObject(fld, "v", ana[i].valuesave);
			cJSON_AddNumberToObject(fld, "q", ana[i].updateflag);
		}
		else if (type == STA_INFO)
		{
			cJSON_AddStringToObject(fld, "id", stat[i].src_name);
			cJSON_AddStringToObject(fld, "cn", stat[i].cnname);
			cJSON_AddNumberToObject(fld, "v", stat[i].valuesave);
			cJSON_AddNumberToObject(fld, "t", stat[i].datetime);
			cJSON_AddNumberToObject(fld, "q", stat[i].updateflag);
		}
		else if (type == STA_DATA)
		{
			cJSON_AddStringToObject(fld, "id", stat[i].src_name);
			cJSON_AddNumberToObject(fld, "v", stat[i].valuesave);
			cJSON_AddNumberToObject(fld, "t", stat[i].datetime);
			cJSON_AddNumberToObject(fld, "q", stat[i].updateflag);
		}

		j++;
		if ((j == BUF_DATANUM) || (i == count - 1))
		{
			out = cJSON_Print(root);
			cJSON_Delete(root);
			if (sendBuffer(serv_sock, out) < 0)
				printf("createSendObjects: Send buffer error\n");
			//ka_produce(connector.rk, connector.rkt, out);
//			printf("%s\n", out);
			free(out);	
			j = 0;
		}
	}
}

int browAnaInfo(sock)
{
	int       stn_num = 0;
	int       group_num = 0;
	int       num=0;
	int i,j,m;
	long rcdcount=0;
	float		fValue;
	ushort updateflag;
	char entryName[512],src_name[128],grp_name[56],stn_name[56],c_name[128];
	time_t timestamp;
	time(&timestamp);
	srand(timestamp);
	for (m = 0; m < MAX_ANA_NUM; m++)
	{
		memset(src_name, 0, 128);
		strcat(src_name, "PDPCSSYSDSIM01");
		sprintf(entryName,"%06d",m);
		strcat(src_name, entryName);
		strcpy(ana[rcdcount].src_name, src_name);
		ana[rcdcount].datetime = timestamp;  //initiate time
		memset(c_name, 0, 128);
		fValue = (rand() % 10000) / 10.0 ;
		ana[rcdcount].valuesave = fValue;
		strcat(c_name, "瀑布沟测试模拟量");
		strcat(c_name, entryName);
		strcpy(ana[rcdcount].cnname, c_name);
		ana[rcdcount].updateflag = 1;
		rcdcount++;
	}

	if(rcdcount == 0) 
		printf("Command Point Name Error! \n");
	else
		createSendObjects(NUM_INFO, rcdcount, ana);
	printf("browAnaData: Send NUM_INFO data %d record!\n",rcdcount);
	return rcdcount;
}

int browAnaData(int num)
{
	uint	chg_number;
	int i,h;
	float		fValue;
	ushort updateflag;
	time_t timestamp;	
	char err_str[80]="";
	ANADATA_STR data[MAX_ANA_NUM];
	time(&timestamp);
	h = 0;
	srand(timestamp);
	for (i = 0; i < num; i++)
	{		
		data[i].val = (rand() % 10000) / 10.0;
		strcpy(data[i].name, ana[i].src_name);
		if (ana[i].valuesave != data[i].val)
		{
//			printf("%s ,ana[%d].valuesave =%f;data[%d].val=%f\n",data[i].name, i, ana[i].valuesave,i, data[i].val);				
			ana[i].valuesave = data[i].val;
			ana[i].datetime = timestamp;
			ana_chg[h].valuesave = data[i].val;
			strcpy(ana_chg[h].src_name, data[i].name);
			ana_chg[h].datetime = timestamp;
			ana_chg[h].updateflag = 1;
			h++;
		}
	}
	chg_number = h;
	createSendObjects(NUM_DATA, chg_number, ana_chg);
	printf("browAnaData: Send ANA data %d record!\n", chg_number);
	return chg_number;
}

int browPolInfo()
{
	int		datatype;
	int       stn_num = 0;
	int       group_num = 0;
	int       num=0;
	int i,j,m;
	ushort state=0;
	long rcdcount=0;
	ushort updateflag;
	char entryName[512],src_name[128],grp_name[56],stn_name[56],c_name[128];
	time_t timestamp;
	time(&timestamp);
	srand(timestamp);
	for (m = 0; m < MAX_POL_NUM; m++)
	{
		memset(src_name, 0, 128);
		memset(c_name, 0, 128);
		strcat(src_name, "PDPCSSYSDSIM00");
		sprintf(entryName, "%06d", m);
		strcat(src_name, entryName);		
		strcpy(pol[rcdcount].src_name, src_name);
		state = rand() % 2;
		pol[rcdcount].datetime = timestamp;
		pol[rcdcount].valuesave = state;
		strcat(c_name, "瀑布沟测试开关量");
		strcat(c_name, entryName);
		strcpy(pol[rcdcount].cnname, c_name);
		pol[rcdcount].updateflag = 1;
		rcdcount++;
	}

	if(rcdcount == 0) 
		printf("Command Station Name Error! \n");
	else
		createSendObjects(STA_INFO, rcdcount, pol);
	printf("browAnaData: Send STA_INFO data %d record!\n", rcdcount);//createSendObjects(NUM_INFO, rcdcount);
	return rcdcount;
}

int browPolData(int num)
{
	uint	chg_number;
	int i,h;
	ushort updateflag;
	time_t timestamp,tmp;
	time(&timestamp);
	char err_str[80]="";
	struct tm timeptr;
	STATDATA_STR pstadata[MAX_POL_NUM];
	h = 0;
	srand(timestamp);
	for (i = 0; i < num; i++)
	{
		pstadata[i].val = (int)(rand() % 2);
		strcpy(pstadata[i].name, pol[i].src_name);
		if (pol[i].valuesave != pstadata[i].val)
		{
//			printf("pol[%d].valuesave =%f;pstadata[%d].val=%f\n", i, pol[i].valuesave,i, pstadata[i].val);				
			pol[i].valuesave = pstadata[i].val;
			pol[i].datetime = timestamp;
			pol_chg[h].valuesave = pstadata[i].val;
			strcpy(pol_chg[h].src_name, pstadata[i].name);
			pol_chg[h].datetime = timestamp;
			pol_chg[h].updateflag = 1;
			h++;
		}
	}
	chg_number = h;
	createSendObjects(STA_DATA, chg_number, pol_chg);
	printf("browAnaData: Send STA_DATA data %d record!\n", chg_number);
	return chg_number;
}

void *ThreadAnaSend (void *data)
{
	int rcdcount=0;
	rcdcount=browAnaInfo();
	if( rcdcount<=0 )
	{
		printf("browAnaInfo: No Data Error!\n");
		exit;
	}
	for(;;)
	{
		browAnaData(rcdcount);
		sleep(2);
	}
}

void *ThreadPolSend (void *data)
{
	int rcdcount=0;
	rcdcount=browPolInfo();
	if( rcdcount<=0 )
	{
		printf("ThreadPolSend: No Data Error!\n");
		exit;
	}
	sleep(1);
	for(;;)
	{
		browPolData(rcdcount);
		sleep(2);
	}
}

int connectServer(char *ip,int port)
{
    int sockfd;
	int	numbers;
    struct sockaddr_in sockaddress;
	int nNetTimeOut = 1000, on; //1 second
	int loop = 0;
	unsigned char opt_val;

	printf("start connect server:\n");
	while ((sockfd = socket(AF_INET, SOCK_STREAM, 0)) == -1)
		sleep(1);
    sockaddress.sin_family = AF_INET;
    sockaddress.sin_port = htons(port);
    sockaddress.sin_addr.s_addr = inet_addr(ip);
    bzero(&(sockaddress.sin_zero),8);
	opt_val = 1;
	on = 1;
	setsockopt(sockfd, SOL_SOCKET, SO_KEEPALIVE, (char *)&on, sizeof(on));
	setsockopt(sockfd, IPPROTO_TCP, TCP_NODELAY, (char *)&opt_val, sizeof(char));
//	setsockopt(sockfd, SOL_SOCKET, SO_SNDTIMEO, (char *)&nNetTimeOut, sizeof(int));
	while(connect(sockfd,(struct sockaddr*)&sockaddress,sizeof(struct sockaddr))==-1)
	{
		if(loop >= MAXTIME_CONNECT) return(-1);
		printf("Fail to connect server\n");
		sleep(1);
	}
    printf("Get the server\n");
	return(sockfd);
}

int sendBuffer(int sock, char *buffer)
{
	int result;
	int error = 0, len = sizeof(int);
	int count;
	struct timeval to;
	fd_set fs;

	result = SOCKET_ERROR;
	if (sock == SOCKET_ERROR)
	{
		printf("sendBuffer():error sock=%d ",sock);
		return SOCKET_ERROR;
	}
	count = strlen(buffer);
	if (count <= 0)
	{
		printf("sendBuffer(): error,count=%d/n",count);
		return SOCKET_ERROR;
	}

	getsockopt(sock, SOL_SOCKET, SO_ERROR, (char *)&error, (int *)&len);
	if (error != 0)
	{
		printf("sendBuffer(): getsock error=%d ",error);
		return SOCKET_ERROR;
	}
	to.tv_sec = 2;
	to.tv_usec = 0;
	FD_ZERO(&fs);
	FD_SET(sock, &fs);
	if(select(sock+1, 0, &fs, 0, &to)>0)
	{
		count = strlen(buffer);
		result = send(sock, (char *)buffer, count, 0);
	}
	else
	{
		printf("net_select result = %d \n",result);
	}
	error = 0;
	getsockopt(sock, SOL_SOCKET, SO_ERROR, (char *)&error, (int *)&len);
	if (error != 0)
	{
		printf("sendBuffer(): getsock after send error=%d ",error);
		return SOCKET_ERROR;
	}
	if (result == SOCKET_ERROR)
	{	
		printf("sendBuffer():error send() %d bytes timeout ",count);	
	}	
		
	return result;
}

int main (int argc, char *argv[])
{
	pthread_t update_ana_threads,
				update_pol_threads;
	void *ret;

	signal(SIGPIPE, SIG_IGN);
	char *ip = "127.0.0.1";
//	char *ip = "192.168.96.1";
	int port = 8000;

	mode = MODE_SEND_CHANGED;
    if((argc==2) &&(strcmp(argv[1],"-all")==0))
    {
    	mode=MODE_SEND_ALL;
    }  
	
	if(mode == MODE_SEND_ALL) 
		printf ("\nSend Mode: MODE_SEND_ALL - All Data Send,No Matter Changed or Not\n\n");
	else
		printf ("\nSend Mode: MODE_SEND_CHANGED - Only Changed Data Send\n\n");

	for(;;)
	{
		serv_sock = connectServer(ip, port);
		if (serv_sock < 0)
		{
			printf("*******************Server connect wrong!**********************\n");
			sleep (2);
		}
		else
			{
				printf("*******************Treads begin!**********************\n");
				break;
			}//
	}

	pthread_mutex_init (&mutex, NULL);
	pthread_create(&update_ana_threads, NULL, ThreadAnaSend, &serv_sock);
	sleep (1);
	pthread_create(&update_pol_threads, NULL, ThreadPolSend, &serv_sock);

	for(;;)
	{
		sleep (5);
	}

	pthread_join (update_ana_threads, &ret);
	pthread_join (update_pol_threads, &ret);

	return 1;
}
