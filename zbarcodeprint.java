package com.printer;

import android.os.Looper;
import android.util.Log;

import com.util.DemoSleeper;
import com.util.UIHelper;
import com.util.Util;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by swq on 2019-6-26.
 * 斑马打印
 */

public class zbarcodeprint {
    private String printerCode = Util.getPringerCode();
    private boolean sendData = true;
    Connection printerConnection = null;
    private UIHelper helper = null;

    public void setHelp (UIHelper helper){
        this.helper  = helper;
    }

    public void executeTest(final boolean withManyJobs) {
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                connectAndSendLabel(withManyJobs);
                Looper.loop();
                Looper.myLooper().quit();
                sendData = true;
            }
        }).start();

    }

    private void connectAndSendLabel(final boolean withManyJobs) {
            printerConnection = new BluetoothConnection(printerCode);
        try {
            helper.showLoadingDialog("Connecting...");
            printerConnection.open();

            ZebraPrinter printer = null;

            if (printerConnection.isConnected()) {
                printer = ZebraPrinterFactory.getInstance(printerConnection);

                if (printer != null) {
                    PrinterLanguage pl = printer.getPrinterControlLanguage();
                    if (pl == PrinterLanguage.CPCL) {
                        helper.showErrorDialogOnGuiThread("This demo will not work for CPCL printers!");
                    } else {
                        // [self.connectivityViewController setStatus:@"Building receipt in ZPL..." withColor:[UIColor
                        // cyanColor]];
                        if (withManyJobs) {
                            sendTestLabelWithManyJobs(printerConnection);
                        } else {
                            sendTestLabel();
                        }
                    }
                    printerConnection.close();
                    //saveSettings();
                }
            }
        } catch (ConnectionException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } catch (ZebraPrinterLanguageUnknownException e) {
            helper.showErrorDialogOnGuiThread("Could not detect printer language");
        } finally {
            helper.dismissLoadingDialog();
        }
    }

    private void tagPrint() {

    }
    private void sendTestLabel() {
        try {
            byte[] configLabel = createZplReceipt().getBytes();
            printerConnection.write(configLabel);
            DemoSleeper.sleep(1500);
            if (printerConnection instanceof BluetoothConnection) {
                DemoSleeper.sleep(500);
            }
        } catch (ConnectionException e) {
        }
    }

    private void sendTestLabelWithManyJobs(Connection printerConnection) {
        try {
            sendZplReceipt(printerConnection);
        } catch (ConnectionException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        }

    }

    private String createZplReceipt() {
        /*
         This routine is provided to you as an example of how to create a variable length label with user specified data.
         The basic flow of the example is as follows

            Header of the label with some variable data
            Body of the label
                Loops thru user content and creates small line items of printed material
            Footer of the label

         As you can see, there are some variables that the user provides in the header, body and footer, and this routine uses that to build up a proper ZPL string for printing.
         Using this same concept, you can create one label for your receipt header, one for the body and one for the footer. The body receipt will be duplicated as many items as there are in your variable data

         */

        String tmpHeader =
        /*
         Some basics of ZPL. Find more information here : http://www.zebra.com/content/dam/zebra/manuals/en-us/printer/zplii-pm-vol2-en.pdf

         ^XA indicates the beginning of a label
         ^PW sets the width of the label (in dots)
         ^MNN sets the printer in continuous mode (variable length receipts only make sense with variably sized labels)
         ^LL sets the length of the label (we calculate this value at the end of the routine)
         ^LH sets the reference axis for printing.
            You will notice we change this positioning of the 'Y' axis (length) as we build up the label. Once the positioning is changed, all new fields drawn on the label are rendered as if '0' is the new home position
         ^FO sets the origin of the field relative to Label Home ^LH
         ^A sets font information
         ^FD is a field description
         ^GB is graphic boxes (or lines)
         ^B sets barcode information
         ^XZ indicates the end of a label
         */

                "^XA" +

                        "^PON^PW400^MNN^LL%d^LH0,0" + "\r\n" +

                        "^FO50,50" + "\r\n" + "^A0,N,70,70" + "\r\n" + "^FD Shipping^FS" + "\r\n" +

                        "^FO50,130" + "\r\n" + "^A0,N,35,35" + "\r\n" + "^FDPurchase Confirmation^FS" + "\r\n" +

                        "^FO50,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDCustomer:^FS" + "\r\n" +

                        "^FO225,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDAcme Industries^FS" + "\r\n" +

                        "^FO50,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDDelivery Date:^FS" + "\r\n" +

                        "^FO225,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FD%s^FS" + "\r\n" +

                        "^FO50,273" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDItem^FS" + "\r\n" +

                        "^FO280,273" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDPrice^FS" + "\r\n" +

                        "^FO50,300" + "\r\n" + "^GB350,5,5,B,0^FS";

        int headerHeight = 325;
        String body = String.format("^LH0,%d", headerHeight);

        int heightOfOneLine = 40;

        float totalPrice = 0;

        Map<String, String> itemsToPrint = createListOfItems();

        int i = 0;
        for (String productName : itemsToPrint.keySet()) {
            String price = itemsToPrint.get(productName);

            String lineItem = "^FO50,%d" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD%s^FS" + "\r\n" + "^FO280,%d" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD$%s^FS";
            totalPrice += Float.parseFloat(price);
            int totalHeight = i++ * heightOfOneLine;
            body += String.format(lineItem, totalHeight, productName, totalHeight, price);

        }

        long totalBodyHeight = (itemsToPrint.size() + 1) * heightOfOneLine;

        long footerStartPosition = headerHeight + totalBodyHeight;

        String footer = String.format("^LH0,%d" + "\r\n" +

                "^FO50,1" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FDTotal^FS" + "\r\n" +

                "^FO175,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FD$%.2f^FS" + "\r\n" +

                "^FO50,130" + "\r\n" + "^A0,N,45,45" + "\r\n" + "^FDPlease Sign Below^FS" + "\r\n" +

                "^FO50,190" + "\r\n" + "^GB350,200,2,B^FS" + "\r\n" +

                "^FO50,400" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,420" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDThanks for choosing us!^FS" + "\r\n" +

                "^FO50,470" + "\r\n" + "^B3N,N,45,Y,N" + "\r\n" + "^FD0123456^FS" + "\r\n" + "^XZ", footerStartPosition, totalPrice);

        long footerHeight = 600;
        long labelLength = headerHeight + totalBodyHeight + footerHeight;

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String dateString = sdf.format(date);

        String header = String.format(tmpHeader, labelLength, dateString);

        String wholeZplLabel = String.format("%s%s%s", header, body, footer);

        return wholeZplLabel;
    }
    private void sendZplReceipt(Connection printerConnection) throws ConnectionException {
        /*
         This routine is provided to you as an example of how to create a variable length label with user specified data.
         The basic flow of the example is as follows

            Header of the label with some variable data
            Body of the label
                Loops thru user content and creates small line items of printed material
            Footer of the label

         As you can see, there are some variables that the user provides in the header, body and footer, and this routine uses that to build up a proper ZPL string for printing.
         Using this same concept, you can create one label for your receipt header, one for the body and one for the footer. The body receipt will be duplicated as many items as there are in your variable data

         */

        String tmpHeader =
        /*
         Some basics of ZPL. Find more information here : http://www.zebra.com

         ^XA indicates the beginning of a label
         ^PW sets the width of the label (in dots)
         ^MNN sets the printer in continuous mode (variable length receipts only make sense with variably sized labels)
         ^LL sets the length of the label (we calculate this value at the end of the routine)
         ^LH sets the reference axis for printing.
            You will notice we change this positioning of the 'Y' axis (length) as we build up the label. Once the positioning is changed, all new fields drawn on the label are rendered as if '0' is the new home position
         ^FO sets the origin of the field relative to Label Home ^LH
         ^A sets font information
         ^FD is a field description
         ^GB is graphic boxes (or lines)
         ^B sets barcode information
         ^XZ indicates the end of a label
         */

                "^XA" +

                        "^POI^PW400^MNN^LL325^LH0,0" + "\r\n" +

                        "^FO50,50" + "\r\n" + "^A0,N,70,70" + "\r\n" + "^FD Shipping^FS" + "\r\n" +

                        "^FO50,130" + "\r\n" + "^A0,N,35,35" + "\r\n" + "^FDPurchase Confirmation^FS" + "\r\n" +

                        "^FO50,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDCustomer:^FS" + "\r\n" +

                        "^FO225,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDAcme Industries^FS" + "\r\n" +

                        "^FO50,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDDelivery Date:^FS" + "\r\n" +

                        "^FO225,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FD%s^FS" + "\r\n" +

                        "^FO50,273" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDItem^FS" + "\r\n" +

                        "^FO280,273" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDPrice^FS" + "\r\n" +

                        "^FO50,300" + "\r\n" + "^GB350,5,5,B,0^FS" + "^XZ";

        int headerHeight = 325;

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String dateString = sdf.format(date);

        String header = String.format(tmpHeader, dateString);

        printerConnection.write(header.getBytes());

        int heightOfOneLine = 40;

        float totalPrice = 0;

        Map<String, String> itemsToPrint = createListOfItems();

        int i = 0;
        for (String productName : itemsToPrint.keySet()) {
            String price = itemsToPrint.get(productName);

            String lineItem = "^XA^POI^LL40" + "^FO50,10" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD%s^FS" + "\r\n" + "^FO280,10" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD$%s^FS" + "^XZ";
            totalPrice += Float.parseFloat(price);
            String oneLineLabel = String.format(lineItem, productName, price);

            printerConnection.write(oneLineLabel.getBytes());

        }

        long totalBodyHeight = (itemsToPrint.size() + 1) * heightOfOneLine;

        long footerStartPosition = headerHeight + totalBodyHeight;

        String footer = String.format("^XA^POI^LL600" + "\r\n" +

                "^FO50,1" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FDTotal^FS" + "\r\n" +

                "^FO175,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FD$%.2f^FS" + "\r\n" +

                "^FO50,130" + "\r\n" + "^A0,N,45,45" + "\r\n" + "^FDPlease Sign Below^FS" + "\r\n" +

                "^FO50,190" + "\r\n" + "^GB350,200,2,B^FS" + "\r\n" +

                "^FO50,400" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,420" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDThanks for choosing us!^FS" + "\r\n" +

                "^FO50,470" + "\r\n" + "^B3N,N,45,Y,N" + "\r\n" + "^FD0123456^FS" + "\r\n" + "^XZ", totalPrice);

        printerConnection.write(footer.getBytes());

    }

    private Map<String, String> createListOfItems() {
        String[] names = {"Microwave Oven", "Sneakers (Size 7)", "XL T-Shirt", "Socks (3-pack)", "Blender", "DVD Movie"};
        String[] prices = {"79.99", "69.99", "39.99", "12.99", "34.99", "16.99"};
        Map<String, String> retVal = new HashMap<String, String>();

        for (int ix = 0; ix < names.length; ix++) {
            retVal.put(names[ix], prices[ix]);
        }
        return retVal;
    }

    public void print(List<printMessage> messages) throws ConnectionException {
        String defaultprinter_zpl = Util.settings.getString("print_page_zpl", "^PW760^LL420^LH0,0^MD10^RP12^PON^PR8,8");
        String tmpHeader =
        /*
         Some basics of ZPL. Find more information here : http://www.zebra.com/content/dam/zebra/manuals/en-us/printer/zplii-pm-vol2-en.pdf

         ^XA indicates the beginning of a label
         ^PW sets the width of the label (in dots)
         ^MNN sets the printer in continuous mode (variable length receipts only make sense with variably sized labels)
         ^LL sets the length of the label (we calculate this value at the end of the routine)
         ^LH sets the reference axis for printing.
            You will notice we change this positioning of the 'Y' axis (length) as we build up the label. Once the positioning is changed, all new fields drawn on the label are rendered as if '0' is the new home position
         ^FO sets the origin of the field relative to Label Home ^LH
         ^A sets font information
         ^FD is a field description
         ^GB is graphic boxes (or lines)
         ^B sets barcode information
         ^XZ indicates the end of a label
         */

                "^XA" + defaultprinter_zpl+ "\r\n" ;
       // tmpHeader += "~TA000";
        //tmpHeader +="~JSN^LT0^MMT^MNW^MTT^PON^PMN";
        //tmpHeader += "^LL51" ;
        //tmpHeader +="^LH0,0";
        tmpHeader +="^JMA";
       // tmpHeader +="^LRN";
       // tmpHeader +="^CI0^BY2,2,38^JUS";

        String str_mess = "";
        for (int i = 0; i < messages.size(); i++) {
            printMessage tmp = messages.get(i);
            if (tmp.getPrintType() == printType.Text) {
                if (tmp.getPrintFontsize()==printFontsize.larger)
                    //drawBigText(tmp.getMessage(), tmp.X(), tmp.Y()+3);
                    str_mess =  "^FO%d,%d" + "\r\n" + "^A0,N,60,60" + "\r\n" + "^FD%s^FS" + "\r\n" ;

                if (tmp.getPrintFontsize()==printFontsize.middle)
                    //drawText(tmp.getMessage(), tmp.X(), tmp.Y()+4);
                    str_mess =  "^FO%d,%d" + "\r\n" + "^A0,N,35,35" + "\r\n" + "^FD%s^FS" + "\r\n" ;

                if (tmp.getPrintFontsize()==printFontsize.samll)
                    //drawSmallText(tmp.getMessage(), tmp.X(), tmp.Y()+2);
                    str_mess =  "^FO%d,%d" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FD%s^FS" + "\r\n" ;
                String header = String.format(str_mess, tmp.X()*8,tmp.Y()*9,tmp.getMessage());
                tmpHeader += header;
            }

            if (tmp.getPrintType() == printType.BarCode) {
                //drawBarcode(tmp.getMessage(), tmp.X(), tmp.Y());
                //tmpHeader +=  String.format(  "^FO%d,%d" + "\r\n" + "^BY3^BCN,50,Y,N,N,A" + "\r\n" + "^FD%s^FS" + "\r\n" , tmp.X()*8,tmp.Y()*9, tmp.getMessage());
                //tmpHeader +=  String.format(  "^FO%d,%d^BY3^BCN,50,N,N,N^FD%s^FS" + "\r\n" , tmp.X()*8,tmp.Y()*9, tmp.getMessage());
                tmpHeader +=  String.format(  "^FO%d,%d^BY2,3^BCN,50,Y,N,N,A^FD%s^FS" + "\r\n" , tmp.X()*8,tmp.Y()*9, tmp.getMessage());
            }


			/*
			 * 用不到下列情况，注释 暂不实现 if (tmp.getPrintType() == printType.Line) {
			 * printer.prn_drawLine(x, 10, x, y, 300); }
			 *
			 * if (tmp.getPrintType() == printType.Bitmap) { //
			 * printer.prn_drawBitmap(new Bitmap, arg1, arg2, arg3, arg4, //
			 * arg5, arg6, arg7, arg8, arg9); } if (tmp.getPrintFangxiang() ==
			 * printFangxiang.水平) { // y += nexty; x += nextx; }
			 *
			 * if (tmp.getPrintFangxiang() == printFangxiang.垂直) { y += nexty; }
			 */
        }
        String line1 =  "^FO1,15" + "\r\n" + "^GB600,0,1,B,0^FS" + "\r\n" ;
        drawLineV(1, 1, 80);
        String line2 =  "^FO1,105" + "\r\n" + "^GB600,0,1,B,0^FS" + "\r\n" ;
        drawLineV(11, 1, 80);
        String line3 =  "^FO1,195" + "\r\n" + "^GB600,0,1,B,0^FS" + "\r\n" ;
        drawLineV(21, 1, 80);
        String line4 =  "^FO1,285" + "\r\n" + "^GB600,0,1,B,0^FS" + "\r\n" ;
        drawLineV(31, 1, 80);
        String line5 =  "^FO1,365" + "\r\n" + "^GB600,0,1,B,0^FS" + "\r\n" ;
        drawLineV(41, 1, 80);

        drawLineH(40, 11, 31);
        String line0 =  "^FO320,105" + "\r\n" + "^GB0,180,1,B,0^FS" + "\r\n" ;
        tmpHeader += line0;
        //tmpHeader += line1;
        tmpHeader += line2;
        tmpHeader += line3;
        tmpHeader += line4;
        //tmpHeader += line5;
        tmpHeader += "^XZ";
        Log.i("tmpHeader" , tmpHeader);
        printerConnection.write(tmpHeader.getBytes());
        // ///////////打印结束//////////////////////////////
    }

    private void drawText(String text, int x, int y) {
        //单词=2个位置
        //zpSDK.zp_draw_text_ex(x, y, text, "黑体", 8, 0, false, false, false);
    }

    private void drawBigText(String text, int x, int y) {
        //zpSDK.zp_draw_text_ex(x, y, text, "黑体", 6, 0, false, false, false);
    }

    private void drawSmallText(String text, int x, int y) {
        //zpSDK.zp_draw_text_ex(x, y, text, "黑体", 3.4, 0, false, false, false);
    }

    private void drawLineH(int x, int y0, int y1) {
        //zpSDK.zp_draw_line(x, y0, x, y1, 2);// 横线 -------------------
    }

    private void drawLineV(int y, int x0, int x1) {
        //zpSDK.zp_draw_line(x0, y, x1, y, 2);// 竖线|||||||||||||
    }

    private void drawBarcode(String text, int x, int y) {
        //zpSDK.zp_draw_barcode(x, y, text, zpSDK.BARCODE_TYPE.BARCODE_CODE128, 6, 2, 0);
    }

    public void printSendPage(String companyname, String invoiceNo,
                              String dest, String pieces, String carton, String orign,String No) {
        final List<printMessage> printdatas = new ArrayList<printMessage>();
        //Log.i("test",companyname +" " + invoiceNo+" " +dest+" " +pieces+" " + carton+" " + orign+" " + No);
        //不用的变量去掉，珍惜内存资源
/*		printMessage company = new printMessage(printType.Text, companyname, 1,
				5, printFontsize.larger);*/

        printMessage invoice = new printMessage(printType.Text, "INVOICE NO:",
                2, 2, printFontsize.samll);
        printMessage INVOICEVALUE = new printMessage(printType.Text, invoiceNo,
                2, 6, printFontsize.larger);

        printMessage DESTINATION = new printMessage(printType.Text,
                "DESTINATION:", 2, 12, printFontsize.samll);
        printMessage DESTINATIONVALUE = new printMessage(printType.Text, dest,
                2, 16, printFontsize.larger);

        printMessage TOTAL = new printMessage(printType.Text,
                "TOTAL no of pieces:", 41, 12, printFontsize.samll);
        printMessage TOTALVALUE = new printMessage(printType.Text, pieces, 41,
                16, printFontsize.middle);

        printMessage CARTON = new printMessage(printType.Text, "CARTON NO:", 41,
                22, printFontsize.samll);
        printMessage CATTONVALUE = new printMessage(printType.Text, carton, 41,
                26, printFontsize.middle);

        printMessage ORIGN = new printMessage(printType.Text, "ORIGN", 2, 22,
                printFontsize.samll);
        printMessage ORIGNVALUE = new printMessage(printType.Text, orign, 2,
                26, printFontsize.larger);

        // printMessage DESTINATION= new printMessage(printType.Text,
        // "DESTINATION", 20, 20, printFontsize.samll);
        printMessage BARVALUE = new printMessage(printType.BarCode, No,
                2, 32, printFontsize.larger);

        printMessage BARtext = new printMessage(printType.Text, No, 2,
                39, printFontsize.samll);

        //printdatas.add(company);

        printdatas.add(invoice);
        printdatas.add(INVOICEVALUE);

        printdatas.add(DESTINATION);
        printdatas.add(DESTINATIONVALUE);

        printdatas.add(TOTAL);
        printdatas.add(TOTALVALUE);

        printdatas.add(CARTON);
        printdatas.add(CATTONVALUE);

        printdatas.add(ORIGN);
        printdatas.add(ORIGNVALUE);

        printdatas.add(BARVALUE);
        //printdatas.add(BARtext);

        //barcodeprint printer = new barcodeprint(statusBox);
        //printer.print(printdatas);

        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                    printerConnection = new BluetoothConnection(printerCode);
                    try {
                        helper.showLoadingDialog("打印...");
                        printerConnection.open();

                        ZebraPrinter printer = null;

                        if (printerConnection.isConnected()) {
                            printer = ZebraPrinterFactory.getInstance(printerConnection);

                            if (printer != null) {
                                PrinterLanguage pl = printer.getPrinterControlLanguage();
                                if (pl == PrinterLanguage.CPCL) {
                                    helper.showErrorDialogOnGuiThread("This demo will not work for CPCL printers!");
                                } else {
                                    // [self.connectivityViewController setStatus:@"Building receipt in ZPL..." withColor:[UIColor
                                    // cyanColor]];
                                    print(printdatas);
                                    helper.dismissLoadingDialog();
                                }
                                printerConnection.close();

                                //saveSettings();
                            }
                        }
                    } catch (ConnectionException e) {
                        helper.showErrorDialogOnGuiThread(e.getMessage());
                    } catch (ZebraPrinterLanguageUnknownException e) {
                        helper.showErrorDialogOnGuiThread("Could not detect printer language");
                    } finally {
                        helper.dismissLoadingDialog();
                    }

                Looper.loop();
                Looper.myLooper().quit();
                sendData = true;
            }
        }).start();

    }
}
