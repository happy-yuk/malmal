import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SceneSegmentation {
    @Metadata(
            mv = {1, 7, 0},
            k = 1,
            d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\u0018\u0000 \u001a2\u00020\u0001:\u0001\u001aB\u0005¢\u0006\u0002\u0010\u0002J\u0018\u0010\u0013\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\u0014\u001a\u00020\u0004H\u0002J\u000e\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0016J\u000e\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u0007\u001a\u00020\bR\u0014\u0010\u0003\u001a\u00020\u0004X\u0086D¢\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006R\u001a\u0010\u0007\u001a\u00020\bX\u0086.¢\u0006\u000e\n\u0000\u001a\u0004\b\t\u0010\n\"\u0004\b\u000b\u0010\fR\u001a\u0010\r\u001a\u00020\u000eX\u0086.¢\u0006\u000e\n\u0000\u001a\u0004\b\u000f\u0010\u0010\"\u0004\b\u0011\u0010\u0012¨\u0006\u001b"},
            d2 = {"Lcom/example/myapplication/SementicSegmentation;", "", "()V", "TAG", "", "getTAG", "()Ljava/lang/String;", "context", "Landroid/content/Context;", "getContext", "()Landroid/content/Context;", "setContext", "(Landroid/content/Context;)V", "module", "Lorg/pytorch/Module;", "getModule", "()Lorg/pytorch/Module;", "setModule", "(Lorg/pytorch/Module;)V", "assetFilePath", "asset", "inference", "Landroid/graphics/Bitmap;", "bitmap", "initialize", "", "Companion", "app_debug"}
    )
    public final class SementicSegmentation {
        public Context context;
        Module module;
        @NotNull
        private final String TAG = "SementicSegmentation";
        public static final boolean IS_NNAPI = false;
        @NotNull
        private static final String MODEL_NAME = "deeplabv3_scripted.pt";
        @NotNull
        public static final com.example.myapplication.SementicSegmentation.Companion Companion = new com.example.myapplication.SementicSegmentation.Companion((DefaultConstructorMarker)null);

        @NotNull
        public final Context getContext() {
            Context var10000 = this.context;
            if (var10000 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("context");
            }

            return var10000;
        }

        public final void setContext(@NotNull Context var1) {
            Intrinsics.checkNotNullParameter(var1, "<set-?>");
            this.context = var1;
        }

        @NotNull
        public final Module getModule() {
            Module var10000 = this.module;
            if (var10000 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("module");
            }

            return var10000;
        }

        public final void setModule(@NotNull Module var1) {
            Intrinsics.checkNotNullParameter(var1, "<set-?>");
            this.module = var1;
        }

        @NotNull
        public final String getTAG() {
            return this.TAG;
        }

        public final void initialize(@NotNull Context context) {
            Intrinsics.checkNotNullParameter(context, "context");
            this.context = context;
            Module var10001 = Module.load(this.assetFilePath(context, MODEL_NAME));
            Intrinsics.checkNotNullExpressionValue(var10001, "Module.load(assetFilePat…Segmentation.MODEL_NAME))");
            this.module = var10001;
            Log.d(this.TAG, "model Loaded");
        }

        @NotNull
        public final Bitmap inference(@NotNull Bitmap bitmap) {
            Intrinsics.checkNotNullParameter(bitmap, "bitmap");
            long time = System.currentTimeMillis();
            Log.d(this.TAG, "start inference");
            Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
            Intrinsics.checkNotNullExpressionValue(inputTensor, "inputTensor");
            float[] inputs = inputTensor.getDataAsFloatArray();
            Module var10000 = this.module;
            if (var10000 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("module");
            }

            Map outTensors = var10000.forward(new IValue[]{IValue.from(inputTensor)}).toDictStringKey();
            Object var32 = outTensors.get("aux");
            Intrinsics.checkNotNull(var32);
            Tensor outputTensor = ((IValue)var32).toTensor();
            String outTensorsKeys = CollectionsKt.joinToString$default((Iterable)outTensors.entrySet(), (CharSequence)", ", (CharSequence)null, (CharSequence)null, 0, (CharSequence)null, (Function1)null.INSTANCE, 30, (Object)null);
            Intrinsics.checkNotNullExpressionValue(outputTensor, "outputTensor");
            float[] outputs = outputTensor.getDataAsFloatArray();
            int width = (int)outputTensor.shape()[2];
            int height = (int)outputTensor.shape()[3];
            int CLASSNUM = 21;
            int BUS = 6;
            int CAR = 7;
            int PERSON = 15;
            int DOG = true;
            int SHEEP = true;
            int[] intValues = new int[width * height];
            StringBuilder maxiList = new StringBuilder();
            int j = 0;

            for(int var21 = width; j < var21; ++j) {
                int k = 0;

                for(int var23 = height; k < var23; ++k) {
                    int maxi = 0;
                    int maxj = 0;
                    int maxk = 0;
                    double maxnum = -100000.0;
                    int maxiIndex = 0;

                    for(byte var30 = CLASSNUM; maxiIndex < var30; ++maxiIndex) {
                        int index = maxiIndex * width * height + j * width + k;
                        if ((double)outputs[index] > maxnum) {
                            maxnum = (double)outputs[index];
                            maxi = maxiIndex;
                            maxj = j;
                            maxk = k;
                        }
                    }

                    maxiList.append(maxi);
                    maxiList.append(", ");
                    maxiIndex = maxj * width + maxk;
                    if (maxi == BUS) {
                        intValues[maxiIndex] = (int)4294901760L;
                    } else if (maxi == CAR) {
                        intValues[maxiIndex] = (int)4278255360L;
                    } else if (maxi == PERSON) {
                        intValues[maxiIndex] = (int)4278190335L;
                    } else {
                        intValues[maxiIndex] = (int)4278190080L;
                    }
                }
            }

            Bitmap bmpSegmentation = Bitmap.createScaledBitmap(bitmap, width, height, true);
            Intrinsics.checkNotNullExpressionValue(bmpSegmentation, "bmpSegmentation");
            Bitmap outputBitmap = bmpSegmentation.copy(bmpSegmentation.getConfig(), true);
            Intrinsics.checkNotNullExpressionValue(outputBitmap, "outputBitmap");
            outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());
            Log.d(this.TAG, "inference end");
            return outputBitmap;
        }

        private final String assetFilePath(Context context, String asset) {
            File file = new File(context.getFilesDir(), asset);

            try {
                InputStream var10000 = context.getAssets().open(asset);
                Intrinsics.checkNotNullExpressionValue(var10000, "context.assets.open(asset)");
                InputStream inpStream = var10000;

                try {
                    FileOutputStream outStream = new FileOutputStream(file, false);
                    byte[] buffer = new byte[4096];
                    int read = false;

                    while(true) {
                        int read = inpStream.read(buffer);
                        if (read == -1) {
                            outStream.flush();
                            break;
                        }

                        outStream.write(buffer, 0, read);
                    }
                } catch (Exception var8) {
                    var8.printStackTrace();
                }

                String var10 = file.getAbsolutePath();
                Intrinsics.checkNotNullExpressionValue(var10, "file.absolutePath");
                return var10;
            } catch (Exception var9) {
                var9.printStackTrace();
                return "";
            }
        }

        @Metadata(
                mv = {1, 7, 0},
                k = 1,
                d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T¢\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u0006X\u0086D¢\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\b¨\u0006\t"},
                d2 = {"Lcom/example/myapplication/SementicSegmentation$Companion;", "", "()V", "IS_NNAPI", "", "MODEL_NAME", "", "getMODEL_NAME", "()Ljava/lang/String;", "app_debug"}
        )
        public static final class Companion {
            @NotNull
            public final String getMODEL_NAME() {
                return com.example.myapplication.SementicSegmentation.MODEL_NAME;
            }

            private Companion() {
            }

            // $FF: synthetic method
            public Companion(DefaultConstructorMarker $constructor_marker) {
                this();
            }
        }
    }

}
