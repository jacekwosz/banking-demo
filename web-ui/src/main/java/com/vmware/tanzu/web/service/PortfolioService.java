package com.vmware.tanzu.web.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.vmware.tanzu.web.domain.CompanyInfo;
import com.vmware.tanzu.web.domain.Order;
import com.vmware.tanzu.web.domain.Portfolio;
import com.vmware.tanzu.web.domain.Quote;
import com.vmware.tanzu.web.exception.OrderNotSavedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.cloud.client.discovery.DiscoveryClient;

//import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;


@Service
@RefreshScope
public class PortfolioService {
	private static final Logger logger = LoggerFactory
			.getLogger(PortfolioService.class);
	@Autowired
	//@LoadBalanced
	private RestTemplate restTemplate;

	@Autowired
    private DiscoveryClient discoveryClient;

    @Value("${portfolioServiceName:portfolio-svc}")
	private String portfolioService;

	@Value("${vmware.tanzu.downstream-protocol:http}")
	protected String downstreamProtocol;

	public Order sendOrder(Order order ) throws OrderNotSavedException{
		logger.debug("send order: " + order);
		
		//check result of http request to ensure its ok.
		String portfolioServiceURI = String.valueOf(discoveryClient.getInstances("portfolio-service").get(0).getUri());
		ResponseEntity<Order>  result = restTemplate.postForEntity(portfolioServiceURI + "/portfolio", order, Order.class);
		if (result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
			throw new OrderNotSavedException("Could not save the order");
		}
		logger.debug("Order saved:: " + result.getBody());
		return result.getBody();
	}
	// The below is commented out to demonstrate impact of lack of hystrix, and can be uncommented during presentation
	//@HystrixCommand(fallbackMethod = "getPortfolioFallback")
	public Portfolio getPortfolio(String user) {
		String portfolioServiceURI = String.valueOf(discoveryClient.getInstances("portfolio-service").get(0).getUri());
		Portfolio folio = restTemplate.getForObject(portfolioServiceURI + "/portfolio/{accountid}", Portfolio.class, user);
		logger.debug("Portfolio received: " + folio);
		return folio;
	}
	
	private Portfolio getPortfolioFallback(String accountId) {
		logger.debug("Portfolio fallback");
		Portfolio folio = new Portfolio();
		folio.setAccountId(accountId);
		return folio;
	}

}
