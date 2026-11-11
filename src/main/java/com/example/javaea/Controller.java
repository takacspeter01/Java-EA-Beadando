package com.example.javaea;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import soapclient.MNBArfolyamServiceSoap;
import soapclient.MNBArfolyamServiceSoapGetExchangeRatesStringFaultFaultMessage;
import soapclient.MNBArfolyamServiceSoapImpl;
import soapclient.*;

@org.springframework.stereotype.Controller
public class Controller<End> {

    @GetMapping("/")
    public String home() {
        return "index";
    }


    //Soap alkalmazas

    @GetMapping("/soap")
    public String soap1(Model model) {
        model.addAttribute("param", new MessagePrice());
        return "soap";
    }

    @PostMapping("/soapResult")
    public String soap2(@ModelAttribute MessagePrice messagePrice, Model model) throws
            MNBArfolyamServiceSoapGetExchangeRatesStringFaultFaultMessage {
        MNBArfolyamServiceSoapImpl impl = new MNBArfolyamServiceSoapImpl();
        MNBArfolyamServiceSoap service = impl.getCustomBindingMNBArfolyamServiceSoap();
        String strOut= "Currency:"+messagePrice.getCurrency()+";"+"Start date:"+messagePrice.getStartDate()+";"+"End date:"+messagePrice.getEndDate()+";";
        strOut+=service.getExchangeRates(messagePrice.getStartDate(),messagePrice.getEndDate(),messagePrice.getCurrency());
        model.addAttribute("sendOut", strOut);
        return "soapResult";
    }



}
