```
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
```
