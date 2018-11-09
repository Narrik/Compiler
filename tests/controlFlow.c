void main(){
int a;
char b;
char *c;
c = "lol";
a = 4;
print_s("okay");
print_s(c);
if (a > 3){
print_s("gt_right ");
}
if (a > 5){
print_s("gt_wrong ");
}
if (a >= 4){
print_s("ge_right ");
}
if (a >= 5){
print_s("ge_wrong ");
}
if (a < 5){
print_s("lt_right ");
}
if (a < 3){
print_s("lt_wrong ");
}
if (a <= 4){
print_s("le_right ");
}
if (a <= 3){
print_s("le_wrong ");
}
if (a != 3){
print_s("ne_right ");
}
if (a != 4){
print_s("ne_wrong ");
}
if (a == 4){
print_s("eq_right ");
}
if (a == 5){
print_s("eq_wrong ");
}
}