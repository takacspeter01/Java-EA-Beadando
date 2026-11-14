package com.example.javaea;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import soapclient.MNBArfolyamServiceSoap;
import soapclient.MNBArfolyamServiceSoapGetExchangeRatesStringFaultFaultMessage;
import soapclient.MNBArfolyamServiceSoapImpl;

import com.oanda.v20.Context;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import com.oanda.v20.primitives.InstrumentName;
import static com.oanda.v20.instrument.CandlestickGranularity.*;

import com.oanda.v20.order.MarketOrderRequest;
import com.oanda.v20.order.OrderCreateRequest;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.order.OrderPositionFill;
import com.oanda.v20.order.TimeInForce;
import com.oanda.v20.trade.Trade;
import com.oanda.v20.trade.TradeCloseRequest;
import com.oanda.v20.trade.TradeCloseResponse;
import com.oanda.v20.trade.TradeSpecifier;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Controller
public class Controller<End> {

    Context ctx = new Context(ForexConfig.URL,ForexConfig.TOKEN);

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


    //Forex

    //!!!!!!!!!FOREX CONFIGBA ACCOUNT IDT ÉS TOKENT BEÍRNI!!!!!!!!!

    @GetMapping("/account_info")
    @ResponseBody
    public AccountSummary f1()  {

        try {
            AccountSummary summary = ctx.account.summary(ForexConfig.ACCOUNTID).getAccount();
            return summary;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //Forex aktuális árak
    @GetMapping("/actual_prices")
    public String actual_prices(Model model) {
        model.addAttribute("par", new ForexMessageActPrice());
        return "form_actual_prices";
    }
    @PostMapping("/actual_prices")
    public String actual_prices2(@ModelAttribute ForexMessageActPrice messageActPrice, Model model) {
        String strOut="";
        List<String> instruments = new ArrayList<>( );
        instruments.add(messageActPrice.getInstrument());
        try {
            PricingGetRequest request = new PricingGetRequest(ForexConfig.ACCOUNTID, instruments);
            PricingGetResponse resp = ctx.pricing.get(request);
            for (ClientPrice price : resp.getPrices())
                strOut+=price+"<br>";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("instr", messageActPrice.getInstrument());
        model.addAttribute("price", strOut);
        return "result_actual_prices";
    }

    //Forex history árak
    @GetMapping("/hist_prices")
    public String hist_prices(Model model) {
        model.addAttribute("param", new ForexMessageHistPrice());
        return "form_hist_prices";
    }

    @PostMapping("/hist_prices")
    public String hist_prices2(@ModelAttribute ForexMessageHistPrice messageHistPrice, Model model) {
        String strOut;
        try {
            InstrumentCandlesRequest request = new InstrumentCandlesRequest(new
                    InstrumentName(messageHistPrice.getInstrument()));
            switch (messageHistPrice.getGranularity()) {
                case "M1": request.setGranularity(M1); break;
                case "H1": request.setGranularity(H1); break;
                case "D": request.setGranularity(D); break;
                case "W": request.setGranularity(W); break;
                case "M": request.setGranularity(M); break;
            }
            request.setCount(Long.valueOf(10)); // utolsó 10 árat kérjünk
            InstrumentCandlesResponse resp = ctx.instrument.candles(request);
            strOut = "";
            for (Candlestick candle : resp.getCandles()) strOut += candle.getTime() + "\t" + candle.getMid().getC() + ";";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("instr", messageHistPrice.getInstrument());
        model.addAttribute("granularity", messageHistPrice.getGranularity());
        model.addAttribute("price", strOut);
        return "result_hist_prices";
    }

    @GetMapping("/forex_nyit")
    public String forexNyit() {
        return "forex_nyit";
    }

    @PostMapping("/forex_nyit")
    public String forexNyitPost(
            @RequestParam String instrument,
            @RequestParam int units,
            Model model) {

        try {
            if (!List.of("EUR_USD", "GBP_USD", "USD_JPY")
                    .contains(instrument)) {
                throw new IllegalArgumentException(
                        "Érvénytelen instrumentum!");
            }

            if (units == 0 || Math.abs((long) units) > 1000) {
                throw new IllegalArgumentException(
                        "A mennyiség -1000 és 1000 között " +
                                "lehet, de nem lehet nulla.");
            }

            MarketOrderRequest order = new MarketOrderRequest()
                    .setInstrument(instrument)
                    .setUnits(Integer.toString(units))
                    .setTimeInForce(TimeInForce.FOK)
                    .setPositionFill(OrderPositionFill.DEFAULT);

            OrderCreateRequest request =
                    new OrderCreateRequest(ForexConfig.ACCOUNTID);
            request.setOrder(order);

            OrderCreateResponse response = ctx.order.create(request);

            if (response.getOrderFillTransaction() != null) {
                model.addAttribute("uzenet",
                        "A piaci megbízás teljesült!");
            } else {
                model.addAttribute("uzenet",
                        "A megbízás nem teljesült. " +
                                "Ellenőrizd az OANDA választ!");
            }

        } catch (Exception e) {
            model.addAttribute("uzenet",
                    "Hiba: " + e.getMessage());
        }

        return "forex_nyit";
    }

    @GetMapping("/forex_poz")
    public String forexPoz(Model model) {

        try {
            List<Trade> trades =
                    ctx.trade.listOpen(
                            ForexConfig.ACCOUNTID).getTrades();

            model.addAttribute("trades", trades);

        } catch (Exception e) {
            model.addAttribute("hiba", e.getMessage());
        }

        return "forex_poz";
    }


    @GetMapping("/forex_zar")
    public String forexZar() {
        return "forex_zar";
    }

    @PostMapping("/forex_zar")
    public String forexZarPost(
            @RequestParam String tradeId,
            Model model) {

        try {
            if (!tradeId.matches("[0-9]+")) {
                throw new IllegalArgumentException(
                        "A Trade ID csak számokat tartalmazhat!");
            }

            TradeCloseRequest request =
                    new TradeCloseRequest(
                            ForexConfig.ACCOUNTID,
                            new TradeSpecifier(tradeId));

            request.setUnits("ALL");

            TradeCloseResponse response =
                    ctx.trade.close(request);

            if (response.getOrderFillTransaction() != null) {
                model.addAttribute("uzenet",
                        "A pozíció sikeresen lezárult!");
            } else {
                model.addAttribute("uzenet",
                        "A zárás nem teljesült. " +
                                "Ellenőrizd az OANDA választ!");
            }

        } catch (Exception e) {
            model.addAttribute("uzenet",
                    "Hiba: " + e.getMessage());
        }

        return "forex_zar";
    }



}
