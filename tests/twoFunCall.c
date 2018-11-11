int minus(int a){
return a-10;
}
int plus(int a){
return a+minus(a);
}

int main(){
int a;
a= 5;
return plus(a);
}