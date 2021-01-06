package com.example.controller;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
 
import org.springframework.stereotype.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.*;

@Controller
public class StockController {

    @RequestMapping("/stock")
    public String stock(Model model) {
        Map<String, Integer> graphData1 = new TreeMap<>();
        Map<String, Integer> graphData2 = new TreeMap<>();
        graphData1.put("2016", 147);
        graphData1.put("2017", 1256);
        graphData1.put("2018", 3856);
        graphData1.put("2019", 9807);
        graphData2.put("2016", 5547);
        graphData2.put("2017", 2756);
        graphData2.put("2018", 9356);
        graphData2.put("2019", 2807);
        model.addAttribute("chartData1", graphData1);
        model.addAttribute("chartData2", graphData2);

        model.addAttribute("stock_symbol", "");

        return "stock";
    }

    @RequestMapping(value="/getStockData", method=RequestMethod.POST)
    public String getStockData(@RequestParam("stock_symbol") String stock_symbol, Model model) throws Exception {
        SimpleDateFormat format1 = new SimpleDateFormat( "yyyy-MM-dd");	// 날짜 포맷팅
		Calendar from = Calendar.getInstance();
		Calendar to = Calendar.getInstance();
		from.add(Calendar.DATE, -180);									// 180일 전

		// 입력받은 stock_symbol을 YahooFinance API에서 검색하여 180일 동안의 주식 정보들을 불러와 List에 저장.
		Stock stock = YahooFinance.get(stock_symbol);
		List<HistoricalQuote> stockHistQuotes = stock.getHistory(from, to, Interval.DAILY);
        
        String keyDate;
        Map<String, BigDecimal> lowData = new TreeMap<>();
        Map<String, BigDecimal> highData = new TreeMap<>();

		// 하한가, 상한가 배열 선언 (주식가격 데이터 타입이 BigDecimal 이므로 똑같은 타입으로 선언)
		BigDecimal[] low = new BigDecimal[stockHistQuotes.size()];
		BigDecimal[] high = new BigDecimal[stockHistQuotes.size()];
		
		// 180일 동안의 하한가, 상한가를 순서대로 배열에 값 저장
		for(int i = 0; i < stockHistQuotes.size(); i++) {
            keyDate = format1.format(stockHistQuotes.get(i).getDate().getTime());
			low[i] = stockHistQuotes.get(i).getLow();
			high[i] = stockHistQuotes.get(i).getHigh();
            lowData.put(keyDate, low[i]);
            highData.put(keyDate, high[i]);
		}

		BigDecimal min_value = new BigDecimal("999999");	// 최저가
		BigDecimal max_value = new BigDecimal("0");			// 최고가
		BigDecimal min_temp = new BigDecimal("999999");		// 최저가 계산용 임시 변수
		BigDecimal max_temp = new BigDecimal("0");			// 최고가 계산용 임시 변수
		int min_index = 0;									// 최저가 인덱스
		int max_index = 0;									// 최고가 인덱스
		
		// 180일 동안의 최저가, 최고가와 그 인덱스를 각각 구함
		for(int i = 0; i < stockHistQuotes.size(); i++) {
			min_temp = min_temp.min(low[i]);
			if(!min_value.equals(min_temp)) {
				min_value = min_temp;
				min_index = i;
			}
			max_temp = max_temp.max(high[i]);
			if(!max_value.equals(max_temp)) {
				max_value = max_temp;
				max_index = i;
			}
		}
		
		BigDecimal profit = max_value.subtract(min_value);				// 이익
		Calendar min_date = stockHistQuotes.get(min_index).getDate();	// 최저가 날짜(Calendar)
		Calendar max_date = stockHistQuotes.get(max_index).getDate();	// 최고가 날짜(Calendar)
		BigDecimal min_value2 = new BigDecimal("999999");				// 최저가 임시변수2
		BigDecimal max_value2 = new BigDecimal("0");					// 최고가 임시변수2
		int min_index2 = 0;												// 최저가 임시 인덱스2
		int max_index2 = 0;												// 최고가 임시 인덱스2
		
		// 최저가 날짜가 최고가 날짜보다 늦다면
		if(min_date.compareTo(max_date) == 1) {
			min_temp = new BigDecimal("999999");
			max_temp = new BigDecimal("0");
			
			// 최저가 날짜 ~ 가장 최근 날짜까지의 최대 이익 (오른쪽 이익)
			for(int i = min_index; i < stockHistQuotes.size(); i++) {
				max_temp = max_temp.max(high[i]);
				if(!max_value2.equals(max_temp)) {
					max_value2 = max_temp;
					max_index2 = i;
				}
			}
			BigDecimal profit_right = max_value2.subtract(min_value);
			
			// 180일 전 ~ 최고가 날짜까지의 최대 이익 (왼쪽 이익)
			for(int i = 0; i <= max_index; i++) {
				min_temp = min_temp.min(low[i]);
				if(!min_value2.equals(min_temp)) {
					min_value2 = min_temp;
					min_index2 = i;
				}
			}
			BigDecimal profit_left = max_value.subtract(min_value2);
			
			// 오른쪽 이익이 왼쪽 이익보다 크거나 같다면
			if(profit_right.compareTo(profit_left) >= 0) {
				// 최고가, 최고가 인덱스, 이익 변수를 덮어쓰기
				max_value = max_value2;
				max_index = max_index2;
				profit = profit_right;
			}
			// 왼쪽 이익이 오른쪽 이익보다 크다면
			else {
				// 최저가, 최저가 인덱스, 이익 변수를 덮어쓰기
				min_value = min_value2;
				min_index = min_index2;
				profit = profit_left;
			}
		}
		
		// 매수, 매도 날짜
		String buying_date = format1.format(stockHistQuotes.get(min_index).getDate().getTime());
		String selling_date = format1.format(stockHistQuotes.get(max_index).getDate().getTime());
		
		// 웹페이지로 변수값 보내기
		model.addAttribute("stock_symbol", stock_symbol);	// US Stock Symbol
		model.addAttribute("buying", buying_date);			// 매수 날짜
		model.addAttribute("selling", selling_date);		// 매도 날짜
		model.addAttribute("low_limit", min_value);			// 최대이익을 산출하는 최저가
		model.addAttribute("high_limit", max_value);		// 최대이익을 산출하는 최고가
		model.addAttribute("profit", profit);				// 최대이익
        
        model.addAttribute("lowData", lowData);
        model.addAttribute("highData", highData);

        return "stock";
    }

}
