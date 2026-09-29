import test from 'node:test';
import assert from 'node:assert/strict';
import {accounts} from '../js/data.js';
import {useCustomerProducts} from '../js/services/api-client.js';

test('upstream customer products use the existing account-card components',()=>{
  useCustomerProducts([{id:3,accountNumber:'19473526745367352158',currency:'AZN',balance:'820.75',type:'SAVING',cards:[{id:3,maskedNumber:'4169 **** **** 3321',brand:'VISA',expiry:'11/26'}]}]);
  assert.deepEqual(accounts.map(({id,type,last4,balance})=>({id,type,last4,balance})),[
    {id:'card-3',type:'card',last4:'3321',balance:820.75},
    {id:'account-3',type:'account',last4:'2158',balance:820.75}
  ]);
});

test('presentation customer restores the approved seven products',()=>{
  const products=[
    [101,'AZN','2450.80'],[102,'AZN','680.25'],[103,'USD','1200.00'],[104,'AZN','-350.00'],
    [105,'AZN','5230.50'],[106,'USD','3400.00'],[107,'EUR','1850.00']
  ].map(([id,currency,balance])=>({id,currency,balance,cards:[]}));
  useCustomerProducts(products);
  assert.deepEqual(accounts.map(item=>item.id),['visa-azn','master-azn','visa-usd','credit','account-azn','account-usd','account-eur']);
});
