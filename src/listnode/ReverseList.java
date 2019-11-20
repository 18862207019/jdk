package listnode;

import base.ListNode;


/**
 * 反转链表
 */
public class ReverseList {

    public static ListNode listNode=null;

    public   ListNode reverseList(ListNode head) {
        // 如果头结点为null直接返回
        if (head == null){
            return head;
        }
        // 递归出去的条件
        if (head.next == null){
            return head;
        }

        // p 代表由于next重新指向之后的产生的新的链表
        // p代表 以原链表的尾节点为头结点的链表
        // 例如: (反转完毕5与4节点链表变成 1->2->3->4<-5）
        ListNode p = reverseList(head.next); // 递归
        // 指针的倒转
        head.next.next = head;
        // 防止链表成环
        head.next = null;
        // 返回每次反转完毕之后的以p(原链表的为节点)为头结点的链表
        return p;
}

    public static ListNode reverseBetween(ListNode head, int m, int n) {
        if (head==null){
            return head;
        }
        ListNode cur = head, prev = null;
        while (m>1){
            prev=cur;
            cur=cur.next;
            m--;
            n--;
        }
        ListNode con = prev, tail = cur;
        ListNode third = null;
        while (n>0){
            third=cur.next;
            cur.next=prev;
            prev=cur;
            cur=third;
            n--;
        }
        if (con!=null){
            con.next=prev;
        }else {
            head=prev;
        }
        tail.next=cur;
        return head;
    }

    public static void main(String[] args) {
        ListNode listNode1 =new ListNode(1);
        ListNode listNode2 =new ListNode(2);
        listNode1.next=listNode2;
        ListNode listNode = reverseBetween(listNode1, 1, 2);
        System.out.println(ReverseList.listNode);
    }









}
