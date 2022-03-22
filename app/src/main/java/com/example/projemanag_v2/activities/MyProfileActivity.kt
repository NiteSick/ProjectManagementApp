package com.example.projemanag_v2.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.projemanag_v2.R
import com.example.projemanag_v2.firebase.FireStoreClass
import com.example.projemanag_v2.models.User
import com.example.projemanag_v2.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firestore.v1.FirestoreGrpc
import kotlinx.android.synthetic.main.activity_my_profile.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import java.io.IOException

@Suppress("DEPRECATION")
class MyProfileActivity : BaseActivity() {



    private var mSelectedImageFileUri : Uri? = null
    private lateinit var mUserDetails : User
    private var mProfileImageURL : String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        setUpActionBar()

        FireStoreClass().loadUserData(this)

        iv_profile_user_image.setOnClickListener {
            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
            ==PackageManager.PERMISSION_GRANTED){
                Constants.showImageChooser(this)

            }else{
                ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                Constants.READ_STORAGE_PERMISSION_CODE)
            }
        }
        btn_update.setOnClickListener {
            if (mSelectedImageFileUri!= null){
                uploadUserImage()
            }else{
                showProgressDialog(resources.getString(R.string.please_wait))

                updateUserProfileData()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.READ_STORAGE_PERMISSION_CODE){
           if (grantResults.isNotEmpty()
               && grantResults[0] == PackageManager.PERMISSION_GRANTED){
               Constants.showImageChooser(this)
           }else{
               Toast.makeText(this,
               "Oops, you just denied the permission for storage." +
                       "You can set it from the settings",
               Toast.LENGTH_SHORT).show()
           }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK
            && requestCode == Constants.PICK_IMAGE_REQUEST_CODE
            && data!!.data != null){
            mSelectedImageFileUri = data.data

            try {
                Glide.with(this@MyProfileActivity)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(iv_profile_user_image)
            }catch (e: IOException){
                e.printStackTrace()
            }
        }
    }


    private fun setUpActionBar(){}
    override fun setSupportActionBar(toolbar: Toolbar?) {
        setSupportActionBar(toolbar_my_profile_activity)
        toolbar_main_activity.setNavigationIcon(R.drawable.ic_action_navigation_menu)

        val actionBar = supportActionBar
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = resources.getString(R.string.my_profile_title)

        }


        toolbar_my_profile_activity.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    fun setUserDataInUI(user : User){

        mUserDetails  = user
        Glide.with(this@MyProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(iv_profile_user_image)

        et_name.setText(user.name)
        et_email.setText(user.email)
        if(user.mobile != 0L){
            et_mobile.setText(user.mobile.toString())
        }

    }

   private  fun updateUserProfileData(){
        val userHashMap = HashMap<String, Any>()


        if (mProfileImageURL.isNotEmpty()&&
                mProfileImageURL!= mUserDetails.image){

            userHashMap[Constants.IMAGE] = mProfileImageURL

        }

        if (et_name.toString() != mUserDetails.name){
            userHashMap[Constants.NAME] = et_name.text.toString()

        }
        if (et_mobile.toString() != mUserDetails.mobile.toString()){
            userHashMap[Constants.MOBILE] = et_mobile.text.toString().toLong()

        }

            FireStoreClass().updateUserProfileData(this, userHashMap)

    }

    private fun uploadUserImage(){
        showProgressDialog(resources.getString(R.string.please_wait))

        if (mSelectedImageFileUri != null){

            val sRef : StorageReference = FirebaseStorage
                .getInstance()
                .reference.child("USER_IMAGE"+
                System.currentTimeMillis()+
                "."+ Constants.getFIleExtensions(this,mSelectedImageFileUri))

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                it ->
                Log.e(
                    "Firebase Image URL",
                    it.metadata!!.reference!!.downloadUrl.toString()
                )
                it.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    it->
                    Log.e(
                        "Downloadable Image URL",
                        it.toString())
                    mProfileImageURL = it.toString()

                    updateUserProfileData()
                }
            }.addOnFailureListener{
                it ->
                Toast.makeText(this@MyProfileActivity,
                it.message,
                Toast.LENGTH_LONG).show()

                hideProgressDialog()
            }
        }
    }
    fun profileUpdateSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

}