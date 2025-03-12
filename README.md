Image Operations
```
Image picker:

MediaPickerHelper.createMediaPicker(this)
                .addBuilder(
                    ImageMediaBuilder()
                        .cropImage()
                        .pickImage()
                        .setSelectionCounts(5)
                        .pickMultipleSuccessStatusListener {
                        }
                        .pickSuccessStatusListener {
                            val bitmap = BitmapFactory.decodeFileDescriptor(contentResolver.openFileDescriptor(it.path ?: Uri.EMPTY, "r")?.fileDescriptor)
                            Log.e("TAG", "onCreate: pickSuccessStatusListener: $it ")
                            val path = it.path
                        }
                        .pickFailStatusListener {

                        })
                .makeDarkTheme()
                .pick()



Image Capture:

MediaPickerHelper.createMediaPicker(this)
                .addBuilder(
                    ImageMediaBuilder()
                        .cropImage()
                        .captureImage()
                        .pickSuccessStatusListener {
                            val uri = it.path
                            val bitmap = BitmapFactory.decodeFileDescriptor(contentResolver.openFileDescriptor(it.path ?: Uri.EMPTY, "r")?.fileDescriptor)
                        }.pickFailStatusListener {

                        })
                .pick()
```

Video Operations:

```
pick video
 MediaPickerHelper.createMediaPicker(this)
                .addBuilder(
                    VideoMediaBuilder()
                        .pickVideo()
                        .setMaxVideoDurationInSeconds(60)
                        .pickSuccessStatusListener {
                            Log.e("TAG", "onCreate: pickSuccessStatusListener ${this.getVideoDuration(it.path)}")
                        }.pickFailStatusListener {
                            Log.e("TAG", "onCreate: pickFailStatusListener")
                        })
                .makeDarkTheme()
                .pick()

Record Video:
MediaPickerHelper.createMediaPicker(this)
                .addBuilder(
                    VideoMediaBuilder()
                        .recordVideo()
                        .setMaxVideoDurationInSeconds(10)
                        .pickSuccessStatusListener {

                        }.pickFailStatusListener {

                        })
                .pick()

```

Audio Operations:
```
MediaPickerHelper.createMediaPicker(this)
                .addBuilder(
                    AudioMediaBuilder()
                        .setSelectionCounts(2)
                        .pickMultipleSuccessStatusListener {
                            Log.e("TAG", "onCreate: pickMultipleSuccessStatusListener : $it")
                        }
                        .pickSuccessStatusListener {
                            Log.e("TAG", "onCreate: pickSuccessStatusListener: $it")
                        }.pickFailStatusListener {

                        })
                .pick()
```
