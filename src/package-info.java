/**
 * Ivy is a software bus
 * It allows any software component to exchange data freekly.
 *
 * The basic principle of a software bus is to ease the rapid implementation of new
 * agents, and to manage a dynamic collection of agents on the bus. Agents connect,
 * send messages, receive messages, and disconnect without hindering the overall
 * functionnality of the bus.
 * Each time an application initializes a connection on the bus, it publishes the
 * list of the messages it has subscribed to and then emits a a ready message.
 *
 * @author Yannick Jestin
 */

package fr.dgac.ivy;
