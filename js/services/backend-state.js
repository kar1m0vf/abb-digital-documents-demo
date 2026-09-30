import { config } from '../config.js';
export const backendEnabled = config.mode === 'backend';
let orders = [];
export const remoteOrders = () => orders;
export function replaceRemoteOrders(value) { orders = value; }
export function rememberRemoteOrder(order) { orders = [order, ...orders.filter(item => item.id !== order.id)]; }
let portal = null;
export const portalState = () => portal;
export function setPortalState(value) { portal = value; }
