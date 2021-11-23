package com.lilcode.aop.p4c03.googlemap.activity

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lilcode.aop.p4c03.googlemap.PostActivity
import com.lilcode.aop.p4c03.googlemap.PostInformation
import com.lilcode.aop.p4c03.googlemap.R
import com.lilcode.aop.p4c03.googlemap.RegisterActivity
import kotlinx.android.synthetic.main.activity_add.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AddActivity : AppCompatActivity() {
    private val MIN_SCALE = 0.85f // 뷰가 몇퍼센트로 줄어들 것인지
    private val MIN_ALPHA = 0.5f // 어두워지는 정도를 나타낸 듯 하다.

    //파이어베이스에 저장해야할 항목들
    var URI_LIST:ArrayList<Uri> = ArrayList()
    var adapter = ImgViewAdapter(URI_LIST) //이거랑

    var calendar = Calendar.getInstance() //이거뺴고
    var year = calendar.get(Calendar.YEAR)
    var month = calendar.get(Calendar.MONTH)
    var day = calendar.get(Calendar.DAY_OF_MONTH)

    lateinit var latitude : String
    lateinit var longitude : String
    lateinit var addressName : String
    lateinit var fullAdress : String

    var fbAuth : FirebaseAuth? = FirebaseAuth.getInstance() //유저정보
    var fbFirestore : FirebaseFirestore? = FirebaseFirestore.getInstance() //데이터베이스
    var storage : FirebaseStorage? = FirebaseStorage.getInstance() //이미지 저장소
    var postInfo = PostInformation()//유저 데이터

    //여기까지
    val getResult = registerForActivityResult(ActivityResultContracts.GetMultipleContents()){ uris:List<Uri> ->
        var writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        var readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if(writePermission == PackageManager.PERMISSION_DENIED || readPermission == PackageManager.PERMISSION_DENIED){
            Log.d("if_checking", "true")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }else{
            Log.d("URI_LIST", uris.toString())
            if(uris.size == 0){
                Log.d("URI", "IS EMPTY")
            } else {
                worm_dot_indicator.visibility = View.VISIBLE
                if(URI_LIST.size+uris.size <= 10){
                    for(i in 0 until uris.size){
                        URI_LIST.add(uris[i])
                    }
                } else {
                    Toast.makeText(this@AddActivity, "사진은 최대 10장까지만 추가 가능합니다.", Toast.LENGTH_LONG).show()
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        latitude = intent.getStringExtra("latitude").toString()
        longitude= intent.getStringExtra("longitude").toString()
        addressName=intent.getStringExtra("name").toString()
        fullAdress=intent.getStringExtra("fulladdress").toString()

        add_location_pick.text=intent.getStringExtra("name")
        add_apply.setOnClickListener {
            //Toast.makeText(this, "빌딩이름 : ${addressName}, 주소 : ${fullAdress} 위도/경도 : ${latitude} ${longitude}", Toast.LENGTH_SHORT).show()
            if(URI_LIST.size == 0){ //이미지 없을때
                uploadFirebase()
            }else { //이미지 있을때
                for (num in 0..URI_LIST.size - 1) {
                    if (URI_LIST.get(num) == null) { //이미지 null일 때
                        break
                    }
                    uploadFirebase(URI_LIST.get(num), num)

                    val postIntent = Intent(this, PostActivity::class.java)
                    startActivity(postIntent)
                }
            }
        }

        var writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        var readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if(writePermission == PackageManager.PERMISSION_DENIED || readPermission == PackageManager.PERMISSION_DENIED){
            Log.d("if_checking", "true")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }

        add_time.setOnClickListener{
            //캘린더뷰 만들기
            //var calendar = Calendar.getInstance()
            //var year = calendar.get(Calendar.YEAR)
            //var month = calendar.get(Calendar.MONTH)
            //var day = calendar.get(Calendar.DAY_OF_MONTH)

            var date_listener = object : DatePickerDialog.OnDateSetListener {
                override fun onDateSet(view: DatePicker?, year_pick: Int, month_pick: Int, dayOfMonth_pick: Int){
                    //날짜 업데이트
                    year = year_pick
                    month = month_pick+1
                    day = dayOfMonth_pick

                    add_time.text= "${year_pick}년 ${month_pick + 1}월 ${dayOfMonth_pick}일"
                }
            }
            var builder = DatePickerDialog(this, date_listener, year, month, day)
            builder.show()
        }


        //viewpager
        viewPager_img.adapter = adapter
        viewPager_img.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager_img.setPageTransformer(ZoomOutPageTransformer())
        worm_dot_indicator.setViewPager2(viewPager_img)
        if(URI_LIST.size == 0){
            worm_dot_indicator.visibility = View.INVISIBLE
        }

        AddImageBtn.setOnClickListener{
            Log.d("button_checking", "true")
            getResult.launch("image/*")
        }
    }
    fun delete_img_fun(view: View){
        val position = viewPager_img.currentItem
        URI_LIST.removeAt(position)
        adapter.notifyItemRemoved(position)
        adapter.notifyItemRangeChanged(position, URI_LIST.size)
        adapter.notifyDataSetChanged()
        if(URI_LIST.size == 0){
            worm_dot_indicator.visibility = View.INVISIBLE
        }
    }

    fun uploadFirebase(realUri: Uri, num : Int){ //파이어베이스 (uri,uri저장 인덱스)
        var fileName = "이미지_${SimpleDateFormat("yyyy년 MM월 dd일 'at' HH:mm:ss ").format(Date())}"+ num.toString() + ".jpg" //이미지 이름
        var imagesRef = storage!!.reference.child(fbAuth?.currentUser!!.email.toString()).child(fileName) //이미지 저장 주소

        uploadFirebase()
        fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString()).document("0").get()
            .addOnSuccessListener { document ->
                var postnum = document.data?.getValue("user_postnum").toString()
                var addpostnum: Int = postnum.toInt()
                addpostnum += 1
                postnum = addpostnum.toString()
                imagesRef.putFile(realUri!!).addOnSuccessListener { //이미지 파일 업로드
                    imagesRef.downloadUrl.addOnSuccessListener { uri -> //이미지 파일 저장 주소 받아오기
                        if (num == 0) fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())
                            ?.document(postnum)?.update("fb_image0", uri.toString())
                        if (num == 1) fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())
                            ?.document(postnum)?.update("fb_image1", uri.toString())
                        if (num == 2) fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())
                            ?.document(postnum)?.update("fb_image2", uri.toString())
                        if (num == 3) fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())
                            ?.document(postnum)?.update("fb_image3", uri.toString())
                        if (num == 4) fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())
                            ?.document(postnum)?.update("fb_image4", uri.toString())
                        if (num == 5) fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())
                            ?.document(postnum)?.update("fb_image5", uri.toString())
                        if (num == 6) fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())
                            ?.document(postnum)?.update("fb_image6", uri.toString())
                        if (num == 7) fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())
                            ?.document(postnum)?.update("fb_image7", uri.toString())
                        if (num == 8) fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())
                            ?.document(postnum)?.update("fb_image8", uri.toString())
                        if (num == 9) fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())
                            ?.document(postnum)?.update("fb_image9", uri.toString())
                    }.addOnFailureListener {
                        Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    fun uploadFirebase(){
        //이미지 이외 데이터 set

        fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString()).document("0").get()
            .addOnSuccessListener { document ->
                var postnum = document.data?.getValue("user_postnum").toString()
                var addpostnum : Int = postnum.toInt()
                addpostnum += 1
                postnum = addpostnum.toString()
                Log.d("postnum: ", "post" + postnum + postnum)

                postInfo.ca_year = year
                postInfo.ca_month = month
                postInfo.ca_day = day
                postInfo.ad_latitude = latitude
                postInfo.ad_longitude = longitude
                postInfo.ad_addressName = addressName
                postInfo.ad_fullAdress  = fullAdress
                //데이터 베이스 저장
                fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())?.document(addpostnum.toString()).set(postInfo).addOnSuccessListener {
                    fbFirestore!!.collection(fbAuth?.currentUser!!.email.toString())?.document("0")?.update("user_postnum",addpostnum)
                    Toast.makeText(this, "Post info success", Toast.LENGTH_SHORT).show()
                }

                Toast.makeText(this, "Post inf", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener(){
                Log.d("fail","dd")
            }
    }


    //zoomout 애니메이션
    inner class ZoomOutPageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            view.apply {
                val pageWidth = width
                val pageHeight = height
                when {
                    position < -1 -> { // [-Infinity,-1)
                        // This page is way off-screen to the left.
                        alpha = 0f
                    }
                    position <= 1 -> { // [-1,1]
                        // Modify the default slide transition to shrink the page as well
                        val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
                        val vertMargin = pageHeight * (1 - scaleFactor) / 2
                        val horzMargin = pageWidth * (1 - scaleFactor) / 2
                        translationX = if (position < 0) {
                            horzMargin - vertMargin / 2
                        } else {
                            horzMargin + vertMargin / 2
                        }

                        // Scale the page down (between MIN_SCALE and 1)
                        scaleX = scaleFactor
                        scaleY = scaleFactor

                        // Fade the page relative to its size.
                        alpha = (MIN_ALPHA +
                                (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
                    }
                    else -> { // (1,+Infinity]
                        // This page is way off-screen to the right.
                        alpha = 0f
                    }
                }
            }
        }
    }
}