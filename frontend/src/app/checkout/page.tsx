'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useCart } from '@/context/CartContext';
import { useAuth } from '@/context/AuthContext';
import { orderApi } from '@/lib/api';
import { Order } from '@/lib/types';

export default function CheckoutPage() {
  const { items, total, clearCart } = useCart();
  const { user } = useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (items.length === 0) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-24 text-center">
        <span className="text-5xl block mb-4">🛒</span>
        <p className="text-lg font-semibold text-gray-700 mb-4">Your cart is empty</p>
        <Link href="/products" className="text-emerald-600 hover:underline">Browse products</Link>
      </div>
    );
  }

  async function handlePlaceOrder() {
    if (!user) {
      router.push('/auth/login?redirect=/checkout');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const order = await orderApi.create(
        user.id,
        items.map(i => ({ productId: i.product.id, quantity: i.quantity }))
      ) as Order;
      clearCart();
      router.push(`/orders?newOrder=${order.id}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to place order. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Checkout</h1>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Order review */}
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Order Review</h2>
          <div className="bg-white rounded-2xl border border-gray-100 divide-y divide-gray-50">
            {items.map(item => (
              <div key={item.product.id} className="flex items-center gap-3 p-4">
                <div className="text-2xl w-10 text-center">{
                  { Mats: '🧘', Equipment: '💪', Apparel: '👕', Accessories: '🎒', Supplements: '🌿' }[item.product.category] ?? '🛍️'
                }</div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900 text-sm truncate">{item.product.name}</p>
                  <p className="text-xs text-gray-500">Qty: {item.quantity}</p>
                </div>
                <span className="font-semibold text-gray-900 text-sm">
                  ${(Number(item.product.price) * item.quantity).toFixed(2)}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Summary & place order */}
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Summary</h2>
          <div className="bg-white rounded-2xl border border-gray-100 p-6">
            <div className="space-y-3 text-sm text-gray-600 mb-4">
              <div className="flex justify-between">
                <span>Subtotal</span>
                <span>${total.toFixed(2)}</span>
              </div>
              <div className="flex justify-between">
                <span>Shipping</span>
                <span className="text-emerald-600">{total >= 75 ? 'Free' : '$8.99'}</span>
              </div>
            </div>
            <div className="border-t border-gray-100 pt-3 flex justify-between font-bold text-gray-900 text-xl mb-6">
              <span>Total</span>
              <span>${(total >= 75 ? total : total + 8.99).toFixed(2)}</span>
            </div>

            {!user && (
              <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 mb-4 text-sm text-amber-700">
                You&apos;ll be prompted to sign in before placing your order.
              </div>
            )}

            {user && (
              <div className="bg-emerald-50 rounded-xl p-3 mb-4 text-sm text-emerald-700">
                Placing order as <span className="font-medium">{user.email}</span>
              </div>
            )}

            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 rounded-xl p-3 mb-4 text-sm">
                {error}
              </div>
            )}

            <button
              onClick={handlePlaceOrder}
              disabled={loading}
              className="w-full bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-400 text-white font-bold py-3 rounded-xl transition-colors flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Placing Order...
                </>
              ) : 'Place Order'}
            </button>

            <Link href="/cart"
              className="block text-center text-sm text-gray-500 hover:text-emerald-700 mt-3 transition-colors">
              Back to Cart
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
