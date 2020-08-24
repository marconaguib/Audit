from shutil import copyfile

f= open('C:\\Users\\Nicolas Martin\\AndroidStudioProjects\\Audit\\app\\src\\main\\res\\layout\\content_securite.xml',"w+")
h = open("header.xml","r")
f.write(h.read())
h.close()
for i in range(6):
    t = open("title.xml","r")
    f.write(t.read().replace("Courant",str(i)).replace("Dernier",str(i-1)).replace("BottomOf=\"@+id/button_image_-1","TopOf=\"parent"))
    t.close()
    for j in range(10) :
        p = open("point.xml","r+")
        if (j==0):
            f.write(p.read().replace("Courant",str(i)+"_"+str(j)).replace("pointDernier","titre"+str(i)))
        else:
            f.write(p.read().replace("Courant",str(i)+"_"+str(j)).replace("Dernier",str(i)+"_"+str(j-1)))
        p.close()
    c = open("commentaires.xml","r")
    f.write(c.read().replace("Courant",str(i)).replace("Dernier",str(i-1)))
    c.close();
copyfile("points_securite.txt", "C:\\Users\\Nicolas Martin\\AndroidStudioProjects\\Audit\\app\\src\\main\\assets\\points_securite.txt")
fo= open("footer.xml","r")
f.write(fo.read())
f.close()
